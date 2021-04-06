(ns controllers.comments
  (:require
    [models.comments :as model]
    [views.comments :as views]
    [xiana.core :as xiana]))

(defn fetch
  [state]
  (xiana/flow->
    (assoc state :view views/comments)
    model/fetch-query
    model/fetch-over-fn))

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
    model/update-over-fn))

(defn delete-comment
  [state]
  (xiana/flow->
    (assoc state :view views/comments)
    model/delete-query
    model/delete-over-fn))

