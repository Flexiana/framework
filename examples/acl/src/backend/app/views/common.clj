(ns views.common
  (:require
    [clojure.data.json :as json])
  (:import
    (java.sql
      Timestamp)))

(defn jasonize
  [m]
  (json/write-str m :value-fn (fn [_ v]
                                (cond
                                  (uuid? v) (str v)
                                  (= Timestamp (type v)) (str v)
                                  :else v))))

(defn response
  [state body]
  (->
    state
    (assoc-in [:response :status] 200)
    (assoc-in [:response :headers "Content-type"] "Application/json")
    (assoc-in [:response :body] (jasonize body))))
