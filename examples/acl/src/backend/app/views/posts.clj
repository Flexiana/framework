(ns views.posts
  (:require
    [clojure.data.json :as json]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)))

(defn jasonize
  [m]
  (json/write-str m
                  :value-fn (fn [_ v]
                              (cond
                                (uuid? v) (str v)
                                (= Timestamp (type v)) (str v)
                                :else v))))

(defn post-view
  [{response-data :response-data :as state}]
  (xiana/ok (->
              state
              (assoc-in [:response :status] 200)
              (assoc-in [:response :headers "Content-type"] "Application/json")
              (assoc-in [:response :body]
                (jasonize {:view-type "single posts"
                           :data      response-data})))))

(defn all-posts
  [{response-data :response-data :as state}]
  (xiana/ok (->
              state
              (assoc-in [:response :status] 200)
              (assoc-in [:response :headers "Content-type"] "Application/json")
              (assoc-in [:response :body]
                (jasonize {:view-type "all posts"
                           :data      response-data})))))

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

(defn render-posts-with-comments
  [data]
  (let [comments (:comments data)
        posts (:posts data)
        group-comments (group-by :comments/post_id comments)]
    {:posts (map (fn [p] (assoc p :comments (filter #(= (:comments/post_id %) (:posts/id p)) comments))) posts)}))

(defn fetch-post-with-comments
  [{{{id :id} :query-params} :request
    response-data :response-data
    :as                      state}]
  (if id
    (xiana/ok (->
                state
                (assoc-in [:response :status] 200)
                (assoc-in [:response :headers "Content-type"] "Application/json")
                (assoc-in [:response :body]
                  (jasonize {:view-type "single posts"
                             :data      (render-posts-with-comments (:db-data response-data))}))))
    (xiana/ok (->
                state
                (assoc-in [:response :status] 200)
                (assoc-in [:response :headers "Content-type"] "Application/json")
                (assoc-in [:response :body]
                  (jasonize {:view-type "Multiple posts"
                             :data      (render-posts-with-comments (:db-data response-data))}))))))

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
