(ns xiana.interceptor
  "Collection of useful interceptors"
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.walk :refer [keywordize-keys]]
    [malli.core :as m]
    [malli.error :as me]
    [malli.transform :as mt]
    [ring.middleware.multipart-params :refer [multipart-params-request]]
    [ring.middleware.params :as middleware.params]
    [xiana.interceptor.muuntaja :as muuntaja]
    [xiana.session :as session]))

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
  {:name  ::params
   :enter (fn [state]
            (let [wrap-params #(keywordize-keys
                                 ((middleware.params/wrap-params identity) %))]
              (update state :request (comp wrap-params multipart-params-request))))})

(defn message                                               ; TODO: remove, use logger
  "This interceptor creates a function that prints predefined message.
  Enter: Print an arbitrary message.
  Leave: Print an arbitrary message."
  [msg]
  {:enter (fn [state] (println msg) state)
   :leave (fn [state] (println msg) state)})

(defn session-user-id
  "This interceptor handles the session user id management.
  Enter: Get the session id from the request header, if
  that operation doesn't succeed a new session is created an associated to the
  current state, otherwise the cached session data is used.
  Leave: Verify if the state has a session id, if so add it to
  the session instance and remove the new session property of the current state.
  The final step is the association of the session id to the response header."

  []
  {:enter
   (fn [{request :request :as state}]
     (let [session-backend (-> state :deps :session-backend)
           session-id (try (parse-uuid
                             (get-in request [:headers :session-id]))
                           (catch Exception _ nil))
           session-data (when session-id
                          (session/fetch session-backend
                                         session-id))]
       (if session-data
         ;; associate session data into state
         (assoc state :session-data session-data)
         ;; else, associate a new session
         (assoc-in
           (assoc-in state [:session-data :session-id] (random-uuid))
           [:session-data :new-session] true))))
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
        (assoc-in (f state role)
                  [:session-data :authorization]
                  auth)))}))

(defn muuntaja
  "Muuntaja encoder/decoder interceptor."
  ([] (muuntaja muuntaja/interceptor))
  ([interceptor] interceptor))

(defn- get-request? [{:keys [request-method]}]
  (= :get request-method))

(def prune-get-request-bodies
  "This interceptor removes bodies from GET requests on Enter."
  {:name  ::prune-get-bodies
   :enter (fn [{:keys [request] :as state}]
            (if (get-request? request)
              (update state :request dissoc :body :body-params)
              state))})

(defn valid? [?schema data]
  (let [value (m/decode ?schema data (mt/transformer
                                       (mt/json-transformer)
                                       (mt/string-transformer)
                                       (mt/strip-extra-keys-transformer)))
        details (m/explain ?schema value)]
    (if (nil? details)
      value
      (throw (ex-info "Request schema validation/coercion error"
                      {:xiana/response {:details (me/humanize details)}})))))

(def coercion
  "On enter: validates request parameters
  On leave: validates response body
  on request error: responds {:status 400, :body \"Request coercion failed\"}
  on response error: responds {:status 400, :body \"Response validation failed\"}"
  {:enter (fn [state]
            (if (= :options (get-in state [:request :request-method]))
              state
              (let [path (get-in state [:request-data :match :path-params])
                    query (get-in state [:request :query-params])
                    form-params (or (not-empty (get-in state [:request :form-params]))
                                    (not-empty (get-in state [:request :multipart-params]))
                                    (not-empty (get-in state [:request :body-params])))
                    method (get-in state [:request :request-method])

                    schemas (merge (get-in state [:request-data :match :data :parameters])
                                   (get-in state [:request-data :match :data method :parameters]))
                    cc (cond-> {}
                         (:path schemas)
                         (assoc :path (valid? (:path schemas) path))

                         (:query schemas)
                         (assoc :query (valid? (:query schemas) query))

                         (:form schemas)
                         (assoc :form (valid? (:form schemas) form-params)))]
                (update-in state [:request :params] merge cc))))})
