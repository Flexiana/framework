(ns controllers.users
  (:require
    [models.users :as model]
    [views.users :as views]
    [xiana.core :as xiana]))

(defn fetch
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/fetch-query))

(defn add
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/add-query))

(defn update-user
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/update-query
    model/update-over-fn))

(defn delete-user
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/delete-query
    model/delete-over-fn))

(defn fetch-with-posts-comments
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-posts-comments)
    model/fetch-with-post-comments-query
    model/fetch-with-post-comments-over-fn))

(defn fetch-with-posts
  [state]
  (xiana/ok
    (assoc state :view views/fetch-posts)
    model/fetch-with-post-query
    model/fetch-with-post-over-fn))
