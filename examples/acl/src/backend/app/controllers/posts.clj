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
  [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (try (xiana/ok (-> (assoc-in state [:session-data :authorization] authorization)
                       (assoc-in [:session :user :id] (UUID/fromString authorization))))
         (catch IllegalArgumentException e (unauthorized state)))
    (unauthorized state)))

(defn add-params
  [state]
  (xiana/ok (clojure.core/update state :http-request #(keywordize-keys ((par/wrap-params identity) %)))))

(defn execute
  [state query]
  (jdbc/execute! (get-in state [:deps :db :datasource]) query))

(defn purify
  [elem]
  (into {} (map (fn [[k v]] {(keyword (last (str/split (name k) #"/"))) v}) elem)))

(defn fetch-user
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
  [{{method :request-method} :http-request :as state}]
  (xiana/ok (assoc state :view (view-map method))))

(def query-map
  {:get    (-> (select :*)
               (from :posts))
   :post   (helpers/update :posts)
   :put    (insert-into :posts)
   :delete (delete-from :posts)})

(defn base-query
  [{{method :request-method} :http-request :as state}]
  (xiana/ok (assoc state :query (query-map method))))

(defn handle-id
  [{{params :params} :http-request query :query :as state}]
  (println "handle-id" params)
  (xiana/ok (if (:id params)
              (assoc state :query (-> query (where [:= :id (:id params)])))
              state)))

(defn view
  [{view :view :as state}]
  (println (:query state))
  (view state))

(defn restriction
  [{{restriction :acl} :response-data query :query :as state}]
  (case restriction
    :own (xiana/ok (assoc state :query (-> query (merge-where [:= :user_id (get-in state [:session :user :id])]))))
    (xiana/ok state)))

(defn controller
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


