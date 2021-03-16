(ns views.posts
  (:require
    [xiana.core :as xiana]))

(defn post-view
  [{{restriction :acl} :response-data :as state}]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    (str "Place of one post, filtered by " restriction)})))

(defn all-posts
  [{response :response-data :as state}]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    (str "Place for all posts, you are able to see: " response)})))

(defn not-allowed
  [state]
  (xiana/error (assoc state :response {:status 401 :body "You don't have right to see posts"})))
