(ns views.posts
  (:require
    [views.comments :as comments]
    [views.common :as c]
    [xiana.core :as xiana]))

(defn post-view
  [{response-data :response-data :as state}]
  (xiana/ok (c/response state {:view-type "Single post"
                               :data      response-data})))

(defn all-posts
  [{response-data :response-data :as state}]
  (xiana/ok (c/response state {:view-type "All posts"
                               :data      response-data})))

(defn not-allowed
  [state]
  (xiana/error (assoc state :response {:status 401 :body "You don't have rights to do this"})))

(defn fetch-posts
  [{{{id :id} :query-params} :request
    :as                      state}]
  (if id
    (xiana/flow->
      state
      post-view)
    (xiana/flow->
      state
      all-posts)))

(defn ->post-view
  [m]
  (select-keys m [:posts/id
                  :posts/user_id
                  :posts/content
                  :posts/creation_time]))

(defn render-posts-with-comments
  [data]
  (->> data
       (group-by ->post-view)
       (map (fn [[k v]]
              (assoc k :comments (->> v (mapv comments/->comment-view)
                                      (remove #(every? nil? (vals %)))))))
       (assoc {} :posts)))

(defn fetch-post-with-comments
  [{{{id :id} :query-params} :request
    response-data            :response-data
    :as                      state}]
  (if id
    (xiana/ok (c/response state {:view-type "Single post with comments"
                                 :data      (render-posts-with-comments (:db-data response-data))}))
    (xiana/ok (c/response state {:view-type "Multiple posts with comments"
                                 :data      (render-posts-with-comments (:db-data response-data))}))))

(defn create-post
  [state]
  (xiana/flow->
    state
    all-posts))

(defn update-post
  [state]
  (xiana/flow->
    state
    all-posts))

(defn delete-post
  [state]
  (xiana/flow->
    state
    all-posts))
