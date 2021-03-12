(ns controllers.posts
  (:require
    [clojure.string :as str]
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

(defn something-else
  [state]
  (println (rreq/body-string (:http-request state)))
  (println (par/params-request (:http-request state)))
  (xiana/ok state))

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
  (xiana/ok state))

(defn update-post
  [state]
  (xiana/ok state))

(defn create-post
  [state]
  (xiana/ok state))

(defn delete-post
  [state]
  (xiana/ok state))

(defn re-router
  [{{method :request-method} :http-request :as state}]
  (case method
    :get (fetch-posts state)
    :post (update-post state)
    :put (create-post state)
    :delete (delete-post state)))

(defn acl-failed
  [state]
  (xiana/error (assoc state :response {:status 401 :body "failed by or-else"})))

(defn controller
  [state]
  (xiana/flow->
    state
    (require-logged-in)
    (fetch-user)
    (acl/is-allowed {:or-else acl-failed})
    (something-else)
    (re-router)
    (views/posts-view)))
