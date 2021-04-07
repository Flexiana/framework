(ns controllers.users
  (:require
    [data-ownership.users :as owner]
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
    owner/update-owner-fn))

(defn delete-user
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-users)
    model/delete-query
    owner/delete-owner-fn))

(defn fetch-with-posts-comments
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-posts-comments)
    model/fetch-with-post-comments-query
    owner/fetch-with-post-comments-owner-fn))

(defn fetch-with-posts
  [state]
  (xiana/ok
    (assoc state :view views/fetch-posts)
    model/fetch-with-post-query
    owner/fetch-with-post-owner-fn))
