(ns controllers.posts
  (:require
    [models.posts :as model]
    [views.posts :as views]
    [xiana.core :as xiana]))

(defn fetch
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-posts)
    model/fetch-query
    model/fetch-over-fn))

(defn add
  [state]
  (xiana/flow->
    (assoc state :view views/update-post)
    model/add-query))

(defn update-post
  [state]
  (xiana/flow->
    (assoc state :view views/update-post)
    model/update-query
    model/update-over-fn))

(defn delete-post
  [state]
  (xiana/flow->
    (assoc state :view views/delete-post)
    model/delete-query
    model/delete-over-fn))

(defn fetch-by-ids
  [state]
  (xiana/flow->
    (assoc state :view views/all-posts)
    model/fetch-by-ids-query
    model/fetch-by-ids-over-fn))

(defn fetch-with-comments
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-post-with-comments)
    model/fetch-with-comments-query
    model/fetch-with-comments-over-fn))
