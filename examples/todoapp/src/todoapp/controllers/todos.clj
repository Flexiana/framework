(ns todoapp.controllers.todos
  (:require
    [todoapp.models.todos :as model]
    [todoapp.views.todos :as views]
    [xiana.core :as xiana]))

(defn fetch
  [state]
  (xiana/flow->
    (assoc state :view views/fetch-todos)
    model/list-query))
