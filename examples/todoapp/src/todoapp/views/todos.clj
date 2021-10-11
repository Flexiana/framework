(ns todoapp.views.todos
  (:require
    [todoapp.views.common :as c]
    [xiana.core :as xiana]))

(defn ->todo-view
  [m]
  (select-keys m [:todos/id
                  :todos/title
                  :todos/is_done]))

(defn todo-list
  [{response-data :response-data :as state}]
  (xiana/ok (c/response state {:view-type "todo-list"
                               :data      {:todos (->> response-data
                                                       :db-data
                                                       (map ->todo-view))}})))

(defn fetch-todos
  [state]
  (xiana/flow->
      state
      todo-list))
