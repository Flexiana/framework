(ns controllers.posts
  (:require
    [models.data-ownership :as owner]
    [models.posts :as model]
    [views.posts :as views]))

(defn fetch
  [state]
  (->
    (assoc state :view views/fetch-posts)
    model/fetch-query
    owner/owner-fn))

(defn add
  [state]
  (model/add-query
    (assoc state
           :view
           views/fetch-posts)))

(defn update-post
  [state]
  (->
    (assoc state :view views/fetch-posts)
    model/update-query
    owner/owner-fn))

(defn delete-post
  [state]
  (->
    (assoc state :view views/fetch-posts)
    model/delete-query
    owner/owner-fn))

(defn fetch-by-ids
  [state]
  (->
    (assoc state :view views/all-posts)
    model/fetch-by-ids-query
    owner/owner-fn))

(defn fetch-with-comments
  [state]
  (->
    (assoc state :view views/fetch-post-with-comments)
    model/fetch-with-comments-query
    owner/owner-fn))
