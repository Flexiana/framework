(ns views.comments
  (:require
    [xiana.core :as xiana]))

(defn comment-view
  [{{restriction :acl} :response-data :as state}]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    (str "Place of one comment, filtered by " restriction)})))

(defn all-comments
  [{{restriction :acl} :response-data :as state}]
  (xiana/ok
    (assoc state
      :response
      {:status  200
       :headers {"Content-Type" "text/plain"}
       :body    (str "Place for all comments, you are able to see: " restriction)})))

(defn not-allowed
  [state]
  (xiana/error (assoc state :response {:status 401 :body "You don't have right to see comments"})))
