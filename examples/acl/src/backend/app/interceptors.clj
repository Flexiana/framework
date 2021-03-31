(ns interceptors
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [framework.acl.core :as acl]
    [framework.components.session.backend :refer [fetch add! delete!]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [next.jdbc :as jdbc]
    [ring.middleware.params :as par]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(def log
  {:enter (fn [s]
            (println "Enter to controller" s)
            (xiana/ok s))
   :leave (fn [s]
            (println "Leave from controller" s)
            (xiana/ok s))})

(defn guest
  [state]
  (xiana/ok (assoc-in state [:session-data :user] {:id   (UUID/randomUUID)
                                                   :role :guest})))

(def require-logged-in
  "Tricky login, session should handle user data"
  {:enter (fn [{req :request :as state}]
            (if-let [authorization (get-in req [:headers :authorization])]
              (try (xiana/ok (-> (assoc-in state [:session-data :authorization] authorization)
                                 (assoc-in [:session-data :user :id] (UUID/fromString authorization))))
                   (catch IllegalArgumentException e (guest state)))
              (guest state)))})

(defn session-middleware
  "Copied from framework, for POC"
  [{request :request :as state}]
  (let [sessions-backend (-> state
                             :deps
                             :session-backend)
        session-id (get-in request [:headers :session-id])
        user (when session-id (fetch sessions-backend session-id))]
    (if user
      (xiana/ok (assoc-in state [:session-data :user] user))
      (xiana/ok (assoc-in state [:session-data :session-id] (UUID/randomUUID))))))

(def session-interceptor
  {:enter (fn [state] (session-middleware state))
   :leave (fn update-session
            [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id (get-in state [:session-data :session-id])]
              (add! sessions-backend session-id (get-in state [:session-data :user]))
              (xiana/ok (assoc-in state [:response :headers "Session-id"] (str session-id)))))})

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
  {:enter (fn [{{u :user} :session-data
                :as       state}]
            (if (>= 2 (count (keys u)))
              (let [query (-> (select :*)
                              (from :users)
                              (where [:= :id (:id u)])
                              sql/format)
                    user (first (execute state query))]
                (if user
                  (xiana/ok (assoc-in state [:session-data :user] (purify user)))
                  (xiana/ok state)))
              (xiana/ok state)))
   :leave (fn [{query    :query
                behavior :behavior
                :as      state}]
            (xiana/ok (reduce (fn [state b]
                                (let [resource (:resource b)
                                      result (execute state (sql/format (resource query)))]
                                  (assoc-in state [:response-data :db-data resource] result))) state behavior)))})

(def view
  {:leave (fn [{behavior :behavior :as state}]
            ((:view (first behavior)) state))})

(def acl-restrict
  {:enter (fn [{behavior :behavior
                :as      state}]
            (reduce (fn [st {resource :resource na :on-deny}]
                      (acl/is-allowed (or (:right st) st) {:resource resource :or-else na}))
              state behavior))
   :leave (fn [{{restriction :acl}    :response-data
                query                 :query
                {{user-id :id} :user} :session-data
                behavior              :behavior
                :as                   state}]
            (xiana/ok (reduce (fn [state b]
                                (let [resource (:resource b)]
                                  (assoc-in state [:query resource] ((:over b) (resource query) user-id (resource restriction)))))
                        state behavior)))})

(def query-builder
  {:leave (fn [{behavior                                          :behavior
                {{user-id :id} :user}                             :session-data
                {{id :id} :query-params form-params :form-params} :request
                :as                                               state}]
            (xiana/ok (reduce (fn [state b]
                                (let [{:keys [:resource
                                              :basic-query
                                              :add-id
                                              :add-body]} b
                                      query (cond-> (basic-query)
                                              id (add-id id)
                                              form-params (add-body form-params user-id))]
                                  (assoc-in state [:query resource] query))) state behavior)))})
