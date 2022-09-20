(ns controllers.users
  (:require
    [models.data-ownership :as owner]
    [models.users :as model]
    [views.users :as views]))

(defn fetch
  [state]
  (model/fetch-query
    (assoc state
           :view
           views/fetch-users)))

(defn add
  [state]
  (model/add-query
    (assoc state
           :view
           views/fetch-users)))

(defn update-user
  [state]
  (->
    (assoc state :view views/fetch-users)
    model/update-query
    owner/owner-fn))

(defn delete-user
  [state]
  (->
    (assoc state :view views/fetch-users)
    model/delete-query
    owner/owner-fn))

(defn fetch-with-posts-comments
  [state]
  (->
    (assoc state :view views/fetch-posts-comments)
    model/fetch-with-post-comments-query
    owner/owner-fn))

(defn fetch-with-posts
  [state]
  (->
    (assoc state :view views/fetch-posts)
    model/fetch-with-post-query
    owner/owner-fn))
