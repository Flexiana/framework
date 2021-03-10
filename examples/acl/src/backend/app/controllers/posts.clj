(ns controllers.posts
  (:require [xiana.core :as xiana]
            [framework.components.acl.core :as acl]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [next.jdbc :as jdbc]
            [clojure.string :as str])
  (:import (java.util UUID)))

(defn index-view
  [{response-data :response-data :as state}]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    (str "Index page " response-data)})))

(defn unauthorized [state]
  (xiana/error (assoc state :response {:status 401 :body "Unauthorized"})))

(defn require-logged-in [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (try (xiana/ok (-> (assoc-in state [:session-data :authorization] authorization)
                       (assoc-in [:session :user :id] (UUID/fromString authorization))))
         (catch IllegalArgumentException e (unauthorized state)))
    (unauthorized state)))

(defn something-else
  [state]
  (xiana/ok state))

(defn execute
  [state query]
  (jdbc/execute! (get-in state [:deps :db :datasource]) query))

(defn purify [elem]
  (into {} (map (fn [[k v]] {(keyword (last (str/split (name k) #"/"))) v}) elem)))

(defn fetch-user [{{{id :id} :user} :session :as state}]
  (let [query (-> (select :*)
                  (from :users)
                  (where [:= :id id])
                  sql/format)
        user (first (execute state query))]
    (if user (xiana/ok (assoc-in state [:session :user] (purify user)))
             (xiana/error (assoc state :response {:status 404 :body "User not found"})))))


(defn controller [state]
  (xiana/flow->
    state
    (require-logged-in)
    (fetch-user)
    (acl/is-allowed)
    (something-else)
    (index-view)))
