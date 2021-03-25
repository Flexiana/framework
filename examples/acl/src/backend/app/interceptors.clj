(ns interceptors
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [framework.components.session.backend :refer [fetch add! delete!]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [next.jdbc :as jdbc]
    [potemkin :refer [import-vars]]
    [ring.middleware.params :as par]
    [xiana.core :as xiana]
    [framework.acl.core :as acl])
  (:import
    (java.util
      UUID)))

(comment
  (import-vars
    [framework.components.app.interceptors
     sample-router-interceptor
     sample-controller-interceptor]))

(def sample-acl-controller-interceptor
  {:enter (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
            (xiana/ok state))})

(def log
  {:enter (fn [s]
            (println "Enter to controller" s)
            (xiana/ok s))
   :leave (fn [s]
            (println "Leave from controller" s)
            (xiana/ok s))})

(defn guest
  [state]
  (xiana/ok (assoc-in state [:deps :session :session-data :user] {:id   (UUID/randomUUID)
                                                                  :role :guest})))

(def require-logged-in
  "Tricky login, session should handle user data"
  {:enter (fn [{req :request :as state}]
            (if-let [authorization (get-in req [:headers :authorization])]
              (try (xiana/ok (-> (assoc-in state [:deps :session :session-data :authorization] authorization)
                                 (assoc-in [:deps :session :session-data :user :id] (UUID/fromString authorization))))
                   (catch IllegalArgumentException e (guest state)))
              (guest state)))})

(defn session-middleware
  "Copied from framework, for POC"
  [{request :request :as state}]
  (let [sessions-backend (-> state
                             :deps
                             :session-backend
                             :session-backend)
        session-id (get-in request [:headers :session-id])
        user (when session-id (fetch sessions-backend session-id))]
    (if user
      (xiana/ok (assoc-in state [:deps :session :session-data :user] user))
      (xiana/ok (assoc-in state [:deps :session :session-data :session-id] (UUID/randomUUID))))))

(def session-interceptor
  {:enter (fn [state] (session-middleware state))
   :leave (fn update-session
            [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend
                                       :session-backend)
                  session-id (get-in state [:deps :session :session-data :session-id])]
              (add! sessions-backend session-id (get-in state [:deps :session :session-data :user])))
            (xiana/ok state))})

(def params
  "Extract parameters from request, should be middleware, or interceptor"
  {:enter (fn [state]
            (xiana/ok
              (clojure.core/update state :request
                                   #(keywordize-keys ((par/wrap-params identity) %)))))})

(defn execute
  "Executes db query"
  [state query]
  (jdbc/execute!
    (get-in state [:deps :db :datasource]) query))

(defn purify
  "Removes namespaces from keywords"
  [elem]
  (into {}
        (map (fn [[k v]] {(keyword (name k)) v}) elem)))

(def db-access
  {:enter (fn [{{{{u :user} :session-data} :session} :deps :as state}]
            (if (>= 2 (count (keys u)))
              (let [query (-> (select :*)
                              (from :users)
                              (where [:= :id (:id u)])
                              sql/format)
                    user (first (execute state query))]
                (if user (xiana/ok (assoc-in state [:deps :session :session-data :user] (purify user)))
                         (xiana/ok state)))
              (xiana/ok state)))
   :leave (fn [{query :query :as state}]
            (println query)
            (let [result (execute state (sql/format query))]
              (println result)
              (xiana/ok (assoc-in state [:response-data :db-data] result))))})

(def view
  {:leave (fn [{view :view :as state}]
            (view state))})

(defn restriction-map
  [query user-id case-v]
  (get {[:own :get]    (-> query (merge-where [:= :user_id user-id]))
        [:own :post]   (-> query (merge-where [:= :user_id user-id]))
        [:own :delete] (-> query (merge-where [:= :user_id user-id]))}
       case-v))

(def acl-restrict
  {:enter (fn [state]
            (acl/is-allowed state {:or-else views.posts/not-allowed}))
   :leave (fn [{{restriction :acl}                               :response-data
                query                                            :query
                {{{{user-id :id} :user} :session-data} :session} :deps
                {method :request-method}                         :request
                :as                                              state}]
            (if-let [new-query (restriction-map query user-id [restriction method])]
              (xiana/ok (assoc state :query new-query))
              (xiana/ok state)))})