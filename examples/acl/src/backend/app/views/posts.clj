(ns views.posts
  (:require [xiana.core :as xiana]))

(defn posts-view
  [{response-data :response-data :as state}]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    (str "Posts page, response data: " response-data)})))