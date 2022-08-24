(ns controllers.comments
  (:require
    [models.comments :as model]
    [models.data-ownership :as owner]
    [views.comments :as views]))

(defn fetch
  [state]
  (->
    (assoc state :view views/comments)
    model/fetch-query
    owner/owner-fn))

(defn add
  [state]
  (model/add-query
    (assoc state
           :view
           views/comments)))

(defn update-comment
  [state]
  (->
    (assoc state :view views/comments)
    model/update-query
    owner/owner-fn))

(defn delete-comment
  [state]
  (->
    (assoc state :view views/comments)
    model/delete-query
    owner/owner-fn))
