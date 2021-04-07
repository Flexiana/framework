(ns data-ownership.users
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [xiana.core :as xiana]))

(defn update-owner-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :id user-id]))
                                   query)))))

(defn delete-owner-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn fetch-with-post-comments-owner-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :users.user_id user-id]))
                                   query)))))

(defn fetch-with-post-owner-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :users.user_id user-id]))
                                   query)))))
