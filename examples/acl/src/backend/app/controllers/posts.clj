(ns controllers.posts
  (:require
    [clojure.core :as core]
    [clojure.string :as str]
    [clojure.walk :refer [keywordize-keys]]
    [framework.acl.core :as acl]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [next.jdbc :as jdbc]
    [ring.middleware.params :as par]
    [ring.util.request :as rreq]
    [views.posts :as views]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn unauthorized
  [state]
  (xiana/error (assoc state :response {:status 401 :body "Unauthorized"})))

(defn require-logged-in
  "Tricky login, session should handle user data"
  [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (try (xiana/ok (-> (assoc-in state [:session-data :authorization] authorization)
                       (assoc-in [:session :user :id] (UUID/fromString authorization))))
         (catch IllegalArgumentException e (unauthorized state)))
    (unauthorized state)))

(defn add-params
  "Extract parameters from request, should be middleware, or interceptor"
  [state]
  (xiana/ok (clojure.core/update state :http-request #(keywordize-keys ((par/wrap-params identity) %)))))

(defn execute
  "Executes db query"
  [state query]
  (jdbc/execute! (get-in state [:deps :db :datasource]) query))

(defn purify
  "Removes namespaces from keywords"
  [elem]
  (into {} (map (fn [[k v]] {(keyword (last (str/split (name k) #"/"))) v}) elem)))

(defn fetch-user
  "Tricky login. Session should handle user data"
  [{{{id :id} :user} :session :as state}]
  (let [query (-> (select :*)
                  (from :users)
                  (where [:= :id id])
                  sql/format)
        user (first (execute state query))]
    (if user (xiana/ok (assoc-in state [:session :user] (purify user)))
        (xiana/error (assoc state :response {:status 404 :body "User not found"})))))

(defn fetch-posts
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(defn update-post
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(defn create-post
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(defn delete-post
  [state]
  (xiana/flow->
    state
    views.posts/all-posts))

(def view-map
  {:get    fetch-posts
   :post   update-post
   :put    create-post
   :delete delete-post})

(defn select-view
  "Inserts view, depends on request-method"
  [{{method :request-method} :http-request :as state}]
  (xiana/ok (assoc state :view (view-map method))))

(def query-map
  "Basic SQL queries by request-method"
  {:get    (-> (select :*)
               (from :posts))
   :post   (helpers/update :posts)
   :put    (insert-into :posts)
   :delete (delete-from :posts)})

(defn base-query
  "Inserting SQL query into state"
  [{{method :request-method} :http-request :as state}]
  (xiana/ok (assoc state :query (query-map method))))

(defn handle-id
  "Add where clause to SQL query if it's present"
  [{{params :params} :http-request query :query :as state}]
  (println "handle-id" params)
  (xiana/ok (if (:id params)
              (assoc state :query (-> query (where [:= :id (:id params)])))
              state)))

(defn view
  "Prepare and run view"
  [{view :view :as state}]
  (println (:query state))
  (view state))

(defn restriction
  "Extends WHERE clause if necessary, to check data-ownership"
  [{{restriction :acl} :response-data query :query :as state}]
  (case restriction
    :own (xiana/ok (assoc state :query (-> query (merge-where [:= :user_id (get-in state [:session :user :id])]))))
    (xiana/ok state)))

(defn controller
  "Example controller for ACL, and DataOwnership"
  [state]
  (xiana/flow->
    state
    require-logged-in
    fetch-user
    (acl/is-allowed {:or-else views.posts/not-allowed})
    add-params
    select-view
    base-query
    handle-id
    restriction
    view))
