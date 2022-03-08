(ns views.comments
  (:require
    [views.common :as common]))

(defn ->comment-view
  [m]
  (select-keys m [:comments/id
                  :comments/post_id
                  :comments/user_id
                  :comments/content
                  :comments/creation_time]))

(defn comments
  [{response-data :response-data :as state}]
  (common/response state {:view-type "comments"
                          :data      {:comments (->> response-data
                                                     :db-data
                                                     (map ->comment-view))}}))
