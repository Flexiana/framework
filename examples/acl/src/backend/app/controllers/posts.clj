(ns controllers.posts
  (:require [xiana.core :as xiana]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [next.jdbc :as jdbc]
            [clojure.string :as str])
  (:import (java.util UUID)))

(defn index-view
  [state]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    "Index page"})))

(defn require-logged-in [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (xiana/ok (-> (assoc-in state [:session-data :authorization] authorization)
                  (assoc-in [:session :user :id] (UUID/fromString authorization))))
    (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))

(defn something-else
  [state]
  (println state)
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


(defn find [state]
  (xiana/flow->
    state
    (require-logged-in)
    (fetch-user)
    (something-else)
    (index-view)))
