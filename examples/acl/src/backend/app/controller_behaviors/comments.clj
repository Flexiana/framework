(ns controller-behaviors.comments
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [views.comments :as views])
  (:import
    (java.util
      UUID)))

(def get-map
  {:resource    :comments
   :view        views/all-comments
   :on-deny     views/not-allowed
   :basic-query (fn []
                  (-> (select :*)
                      (from :comments)))
   :add-id      (fn [query id]
                  (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def put-map
  {:resource    :comments
   :view        views/all-comments
   :on-deny     views/not-allowed
   :basic-query (fn [] (insert-into :comments))
   :add-id      (fn [query _] query)
   :add-body    (fn [query
                     {post-id :post_id
                      content :content}
                     user-id]
                  (-> query (columns :content :post_id :user_id) (values [[content (UUID/fromString post-id) user-id]])))
   :over        (fn [query _ _] query)})

(def post-map
  {:resource    :comments
   :view        views/all-comments
   :on-deny     views/not-allowed
   :basic-query (fn [] (helpers/update :comments))
   :add-id      (fn [query id] (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query form-params _] (-> query (sset {:content (:content form-params)})))
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def delete-map
  {:resource    :comments
   :view        views/all-comments
   :on-deny     views/not-allowed
   :basic-query (fn [] (delete-from :comments))
   :add-id      (fn [query id] (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def multi-get-map
  {:resource    :comments
   :view        views/all-comments
   :on-deny     views/not-allowed
   :basic-query (fn []
                  (-> (select :*)
                      (from :comments)))
   :add-id      (fn [query _] query)
   :add-body    (fn [query form-params _]
                  (if-let [ids (:ids form-params)]
                    (-> query (where [:in :id (map #(UUID/fromString %) (if (coll? ids) ids [ids]))]))
                    query))
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})
