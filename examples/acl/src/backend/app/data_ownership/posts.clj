(ns data-ownership.posts
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [xiana.core :as xiana]))

(def ownership
  {:own (fn [query user-id over]
          (-> query (merge-where [:= :posts.user_id user-id])))})

(defn owner-fn
  [state]
  (xiana/ok (assoc state
              :owner-fn
              (ownership (get-in state [:response-data :acl])))))
