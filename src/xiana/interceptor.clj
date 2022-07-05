(ns xiana.interceptor
  "Collection of useful interceptors"
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.params :as middleware.params]
    [xiana.interceptor.muuntaja :as muuntaja]
    [xiana.jwt :as jwt]
    [xiana.route.helpers :as helpers]
    [xiana.session :as session])
  (:import
    (java.util
      UUID)))

(def log
  "Log interceptor.
  Enter: Print 'Enter:' followed by the complete state map.
  Leave: Print 'Leave:' followed by the complete state map."
  {:enter (fn [state] (pprint ["Enter: " state]) state)
   :leave (fn [state] (pprint ["Leave: " state]) state)})

(def side-effect
  "Side-effect interceptor.
  Enter: nil.
  Leave: apply `:side-effect` state key to state if it exists."
  {:name ::side-effect
   :leave
   (fn [state]
     (if-let [f (get state :side-effect)]
       (f state)
       state))})

(def view
  "View interceptor.
  Enter: nil. Leave: apply `:view` state key to state if it exists and
  `:response` is absent."
  {:leave
   (fn [{:keys [view response] :as state}]
     (if (and (not (:body response)) view)
       (view state)
       state))})

;; Question: should be used in the early chain of interceptors?
(def params
  "Update the request map with parsed url-encoded parameters.
  Adds the following keys to the request map:

  :query-params - a map of parameters from the query string
  :form-params  - a map of parameters from the body
  :params       - a merged map of all types of parameter

  Enter: TODO.
  Leave: nil."
  {:name ::params
   :enter (fn [state]
            (let [f #(keywordize-keys
                       ((middleware.params/wrap-params identity) %))]
              (update state :request f)))})

(defn message ; TODO: remove, use logger
  "This interceptor creates a function that prints predefined message.
  Enter: Print an arbitrary message.
  Leave: Print an arbitrary message."
  [msg]
  {:enter (fn [state] (println msg) state)
   :leave (fn [state] (println msg) state)})

(defn session-user-id
  "This interceptor handles the session user id management.
  Enter: Get the session id from the request header, if
  that operation doesn't succeeds a new session is created an associated to the
  current state, otherwise the cached session data is used.
  Leave: Verify if the state has a session id, if so add it to
  the session instance and remove the new session property of the current state.
  The final step is the association of the session id to the response header."

  []
  {:enter
   (fn [{request :request :as state}]
     (let [session-backend (-> state :deps :session-backend)
           session-id (try (UUID/fromString
                             (get-in request [:headers :session-id]))
                           (catch Exception _ nil))
           session-data (when session-id
                          (session/fetch session-backend
                                         session-id))]
       (if session-data
         ;; associate session data into state
         (assoc state :session-data session-data)
         ;; else, associate a new session
         (-> (assoc-in state [:session-data :session-id] (UUID/randomUUID))
             (assoc-in [:session-data :new-session] true)))))
   :leave
   (fn [state]
     (let [session-backend (-> state :deps :session-backend)
           session-id (get-in state [:session-data :session-id])]
       ;; add the session id to the session instance and
       ;; dissociate the new-session from the current state
       (session/add! session-backend
                     session-id
                     (dissoc (:session-data state) :new-session))
       ;; associate the session id
       (assoc-in state
                 [:response :headers :session-id]
                 (str session-id))))})

(defn -user-role
  "Update the user role."
  [state role]
  (assoc-in state [:session-data :user] {:role role}))

(defn session-user-role
  "This interceptor updates session data user role:authorization
  from the given request header.
  Enter: Fetch the authorization from its request/state
  if succeeds update the current state with that information,
  also update the user role with a custom value or the default :guest.
  Leave: nil."
  ([]
   (session-user-role -user-role :guest))
  ([f role]
   {:enter
    (fn [{request :request :as state}]
      (let [auth (get-in request [:headers :authorization])]
        (->
          ;; f: function to update/associate the user role
          (f state role)
          ;; associate authorization into session-data
          (assoc-in [:session-data :authorization] auth))))}))

(defn muuntaja
  "Muuntaja encoder/decoder interceptor."
  ([] (muuntaja muuntaja/interceptor))
  ([interceptor] interceptor))

(def jwt-auth
  {:name ::jwt-authentication
   :enter
   (fn [{request :request :as state}]
     (let [auth (get-in request [:headers :authorization])
           cfg (get-in state [:deps :xiana/jwt :auth])]
       (try
         (jwt/verify-jwt :auth auth cfg)
         state
         (catch clojure.lang.ExceptionInfo e
           (assoc state :error e)))))
   :error
   (fn [state]
     (let [error (:error state)
           err-info (ex-data error)]
       (cond
         (= :exp (:cause err-info))
         (helpers/unauthorized state "JWT Token expired.")
         (= :validation (:type err-info))
         (helpers/unauthorized state "One or more Claims were invalid.")
         :else
         (helpers/unauthorized state "Signature could not be verified."))))})

(def jwt-content
  {:name ::jwt-content-exchange
   :enter
   (fn [{request :request :as state}]
     (if-let [body-params (:body-params request)]
       (let [cfg (get-in state [:deps :xiana/jwt :content])]
         (try
           (->> (jwt/verify-jwt :content body-params cfg)
                (assoc-in state [:request :form-params]))
           (catch clojure.lang.ExceptionInfo e
             (assoc state :error e))))
       state))
   :leave
   (fn [{response :response :as state}]
     (let [cfg (get-in state [:deps :xiana/jwt :content])]
       (->> (jwt/sign :content (:body response) cfg)
            (assoc-in state [:state :response :body]))))
   :error
   (fn [state]
     (helpers/unauthorized state "Signature could not be verified"))})
