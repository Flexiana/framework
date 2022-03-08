(ns views.posts
  (:require
    [views.comments :as comments]
    [views.common :as c]))

(defn ->post-view
  [m]
  (select-keys m [:posts/id
                  :posts/user_id
                  :posts/content
                  :posts/creation_time]))

(defn post-view
  [{response-data :response-data :as state}]
  (c/response state {:view-type "Single post"
                     :data      {:posts (map ->post-view (:db-data response-data))}}))

(defn all-posts
  [{response-data :response-data :as state}]
  (c/response state {:view-type "All posts"
                     :data      {:posts (map ->post-view (:db-data response-data))}}))

(defn fetch-posts
  [{{{id :id} :query-params} :request
    :as                      state}]
  (if id
    (post-view state)
    (all-posts state)))

(defn render-posts-with-comments
  [data]
  (->> data
       (group-by ->post-view)
       (map (fn [[k v]]
              [k (->> v (mapv comments/->comment-view))]))
       (map (fn [[k v]] (assoc k :comments (remove #(every? nil? (vals %)) v))))))

(defn fetch-post-with-comments
  [{{{id :id} :query-params} :request
    response-data            :response-data
    :as                      state}]
  (if id
    (c/response state {:view-type "Single post with comments"
                       :data      {:posts (render-posts-with-comments (:db-data response-data))}})
    (c/response state {:view-type "Multiple posts with comments"
                       :data      {:posts (render-posts-with-comments (:db-data response-data))}})))

