(ns controllers.comments
  (:require
    [models.comments :as model]
    [models.data-ownership :as owner]
    [views.comments :as views]
    [xiana.core :as xiana]))

(defn fetch
  [state]
  (xiana/flow->
    (assoc state :view views/comments)
    model/fetch-query
    owner/owner-fn))

(defn add
  [state]
  (xiana/flow->
    (assoc state :view views/comments)
    model/add-query))

(defn update-comment
  [state]
  (xiana/flow->
    (assoc state :view views/comments)
    model/update-query
    owner/owner-fn))

(defn delete-comment
  [state]
  (xiana/flow->
    (assoc state :view views/comments)
    model/delete-query
    owner/owner-fn))
