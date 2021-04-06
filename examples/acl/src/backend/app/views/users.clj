(ns views.users
  (:require
    [views.common :as c]
    [views.posts :as posts]
    [xiana.core :as xiana]))

(defn ->user
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
                                                       (map ->user))}})))

(defn multi-user
  [{response-data :response-data :as state}]
  (xiana/ok (c/response state {:view-type "multiple users"
                               :data      {:users (->> response-data
                                                       :db-data
                                                       (map ->user))}})))

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
       (group-by ->user)
       (map (fn [[k v]] (assoc k :posts (mapv posts/render-posts-with-comments v))))
       (assoc {} :users)))

(defn fetch-posts-comments
  [{{{id :id} :query-params} :request
    response-data            :response-data
    :as                      state}]
  (if id
    (xiana/ok (c/response state {:view-type "single user with posts"
                                 :data      (render-posts-with-comments (:db-data response-data))}))
    (xiana/ok (c/response state {:view-type "multiple users with posts"
                                 :data      (render-posts-with-comments (:db-data response-data))}))))

(defn render-posts
  [data]
  (->> data
       (group-by ->user)
       (map (fn [[k v]] (assoc k :posts (mapv posts/->post-view v))))
       (assoc {} :users)))

(defn fetch-posts
  [{{{id :id} :query-params} :request
    response-data            :response-data
    :as                      state}]
  (if id
    (xiana/ok (c/response state {:view-type "single user with posts"
                                 :data      (render-posts (:db-data response-data))}))
    (xiana/ok (c/response state {:view-type "multiple users with posts"
                                 :data      (render-posts (:db-data response-data))}))))
