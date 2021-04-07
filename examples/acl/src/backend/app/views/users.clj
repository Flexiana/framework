(ns views.users
  (:require
    [views.common :as c]
    [views.posts :as posts]
    [xiana.core :as xiana]))

(defn ->user-view
  [m]
  (select-keys m [:users/id
                  :users/last_login
                  :users/username
                  :users/first_name
                  :users/last_name
                  :users/email
                  :users/date_joined
                  :users/is_active]))

(defn one-user
  [{response-data :response-data :as state}]
  (xiana/ok (c/response state {:view-type "single user"
                               :data      {:users (->> response-data
                                                       :db-data
                                                       (map ->user-view))}})))

(defn multi-user
  [{response-data :response-data :as state}]
  (xiana/ok (c/response state {:view-type "multiple users"
                               :data      {:users (->> response-data
                                                       :db-data
                                                       (map ->user-view))}})))

(defn fetch-users
  [{{{id :id} :query-params} :request
    :as                      state}]
  (if id
    (xiana/flow->
      state
      one-user)
    (xiana/flow->
      state
      multi-user)))

(defn render-posts-with-comments
  [data]
  (->> data
       (group-by ->user-view)
       (map (fn [[k v]] [k (posts/render-posts-with-comments v)]))
       (map (fn [[k v]] (assoc k :posts v)))))

(defn fetch-posts-comments
  [{{{id :id} :query-params} :request
    response-data            :response-data
    :as                      state}]
  (if id
    (xiana/ok (c/response state {:view-type "single user with posts and comments"
                                 :data      {:users (render-posts-with-comments (:db-data response-data))}}))
    (xiana/ok (c/response state {:view-type "multiple users with posts and comments"
                                 :data      {:users (render-posts-with-comments (:db-data response-data))}}))))

(defn render-posts
  [data]
  (->> data
       (group-by ->user-view)
       (map (fn [[k v]] (assoc k :posts (map posts/->post-view v))))))

(defn fetch-posts
  [{{{id :id} :query-params} :request
    response-data            :response-data
    :as                      state}]
  (if id
    (xiana/ok (c/response state {:view-type "single user with posts"
                                 :data      {:users (render-posts (:db-data response-data))}}))
    (xiana/ok (c/response state {:view-type "multiple users with posts"
                                 :data      {:users (render-posts (:db-data response-data))}}))))
