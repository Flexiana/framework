(ns views.comments
  (:require
    [clojure.data.json :as json]
    [xiana.core :as xiana])
  (:import
    (java.sql
      Timestamp)))

(defn ->comment
  [m]
  (select-keys m [:comments/id
                  :comments/post_id
                  :comments/user_id
                  :comments/content
                  :comments/creation_time]))

(defn jasonize
  [m]
  (json/write-str m
                  :value-fn (fn [_ v]
                              (cond
                                (uuid? v) (str v)
                                (= Timestamp (type v)) (str v)
                                :else v))))

(defn all-comments
  [{response-data :response-data :as state}]
  (xiana/ok (->
              state
              (assoc-in [:response :status] 200)
              (assoc-in [:response :headers "Content-type"] "Application/json")
              (assoc-in [:response :body]
                (jasonize {:view-type "single posts"
                           :data      response-data})))))

(defn not-allowed
  [state]
  (xiana/error (assoc state :response {:status 401 :body "You don't have right to see comments"})))
