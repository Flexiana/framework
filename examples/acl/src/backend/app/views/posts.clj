(ns views.posts
  (:require
    [clojure.data.json :as json]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)))

(defn post-view
  [{response-data :response-data :as state}]
  (xiana/ok (->
              state
              (assoc-in [:response :status] 200)
              (assoc-in [:response :headers "Content-type"] "Application/json")
              (assoc-in [:response :body]
                (json/write-str {:view-type "all posts"
                                 :data      response-data}
                                :value-fn (fn [_ v]
                                            (cond
                                              (uuid? v) (str v)
                                              (= Timestamp (type v)) (str v)
                                              :else v)))))))

(defn all-posts
  [{response-data :response-data :as state}]
  (xiana/ok (->
              state
              (assoc-in [:response :status] 200)
              (assoc-in [:response :headers "Content-type"] "Application/json")
              (assoc-in [:response :body]
                (json/write-str {:view-type "all posts"
                                 :data      response-data}
                                :value-fn (fn [_ v]
                                            (cond
                                              (uuid? v) (str v)
                                              (= Timestamp  (type v)) (str v)
                                              :else v)))))))

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
