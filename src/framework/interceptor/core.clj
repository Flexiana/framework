(ns framework.interceptor.core
  (:require
   [xiana.core :as xiana]
   [honeysql.core :as sql]
   [framework.db.sql :as db]
   [framework.acl.core :as acl]
   [framework.session.core :as session]
   [framework.interceptor.wrap :as wrap]
   [clojure.walk :refer [keywordize-keys]]
   [framework.interceptor.muuntaja :as muuntaja]
   [ring.middleware.params :as middleware.params])
  (:import
   (java.util UUID)))

(def log
  "Log interceptor."
  {:enter (fn [state] (println "Enter: " state) (xiana/ok state))
   :leave (fn [state] (println "Leave: " state) (xiana/ok state))})

(def side-effect
  "Side-effect interceptor."
  {:leave
   (fn [{f :side-effect :as state}]
     (if f (f state) (xiana/ok state)))})

(def view
  "View interceptor."
  {:leave
   (fn [{view :view :as state}]
     (let [f (or view xiana/ok)]
       (f state)))})

(def params
  "Extract parameters from request, should be middleware, or interceptor."
  {:enter (fn [state]
            (let [f #(keywordize-keys
                      ((middleware.params/wrap-params identity) %))]
              (xiana/ok
               (update state :request f))))})

(defn message
  "Message interceptor."
  [msg]
  {:enter (fn [state] (println msg) (xiana/ok state))
   :leave (fn [state] (println msg) (xiana/ok state))})

(defn session-interceptor
  "Session interceptor."
  ([] (session-interceptor (session/init-in-memory)))
  ([session-instance]
   {:enter
    (fn [{request :request :as state}]
      (let [session-id (try (UUID/fromString
                             (get-in request [:headers :session-id]))
                            (catch Exception _ nil))
            session-data (when session-id
                           (session/fetch session-instance
                                          session-id))]
        (xiana/ok
         (if session-id
           ;; associate session in state
           (assoc state :session-data session-data)
           ;; new session
           (-> (assoc-in state [:session-data :session-id] (UUID/randomUUID))
               (assoc-in [:session-data :new-session] true))))))
    :leave
    (fn [state]
      (let [session-id (get-in state [:session-data :session-id])]
        ;; dissociate session data
        (session/add! session-instance
                      session-id
                      (dissoc (:session-data state) :new-session))
        ;; associate the session id
        (xiana/ok
         (assoc-in state
                   [:response :headers "Session-id"]
                   (str session-id)))))}))

(defn- user-role
  "Associate user role to the context."
  [state role]
  (xiana/ok
   (assoc-in state
             [:session-data :user]
             {:id (UUID/randomUUID) :role role})))

(defn require-logged-in
  "Require logged user interceptor."
  ([] (require-logged-in user-role :guest))
  ([user-role-fn role]
   {:enter
    (fn [{request :request :as state}]
      (if-let [authorization (get-in request [:headers :authorization])]
        (try
          (xiana/ok
           (->
            ;; associate authorization to the state
            (assoc-in state [:session-data :authorization] authorization)
            ;; associate user id to the state
            (assoc-in [:session-data :user :id]
                      (UUID/fromString authorization))))
          ;; handle illegal argument exception
          (catch IllegalArgumentException _ (user-role-fn state)))
        ;; else: call the user role function
        (user-role-fn state role)))}))

(defn db-access
  "Database access interceptor."
  ([] {:leave
       (fn [{query :query :as state}]
         (if query
           ;; update state with the result of the database operation
           (xiana/ok
            (let [result (db/execute (sql/format query))]
              (assoc-in state [:response-data :db-data] result)))
           ;; return the container state
           (xiana/ok state)))})
  ;; associate the :enter function to the db-access interceptor
  ([on-new-session]
   (assoc (db-access)
          :enter (fn [{{new-session :new-session} :session-data :as state}]
                   (if new-session
                     (on-new-session state)
                     (xiana/ok state))))))

(defn muuntaja
  "Muuntaja encoder/decoder interceptor."
  ([] (muuntaja muuntaja/interceptor))
  ([interceptor] (wrap/interceptor interceptor)))

(defn acl-restrict
  "Access control layer interceptor.
  Enter function checks access control.
  Leave function place for tightening db query via provided owner-fn."
  ([] (acl-restrict {}))
  ([m]
   {:enter
    (fn [{acl :acl/access-map :as state}]
      (acl/is-allowed state
                      (merge m acl)))
    :leave
    (fn [{query                 :query
          {{user-id :id} :user} :session-data
          owner-fn              :owner-fn
          :as                   state}]
      (xiana/ok
       (if owner-fn
         (assoc state :query (owner-fn query user-id))
         state)))}))
