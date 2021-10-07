(ns models.data-ownership
  (:require
    [honeysql.helpers :refer [merge-where] :as helpers]
    [xiana.core :as xiana]))

(def ownership
  {[:comments :own] (fn [query user-id]
                      (-> query (merge-where [:= :comments.user_id user-id])))
   [:users :own]    (fn [query user-id]
                      (-> query (merge-where [:= :users.id user-id])))
   [:posts :own]    (fn [query user-id]
                      (-> query (merge-where [:= :posts.user_id user-id])))})

(defn owner-fn
  [{response-data :response-data :as state}]
  (xiana/ok state))
