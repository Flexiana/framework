(ns controller-behaviors.posts
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [views.posts :as views])
  (:import
    (java.util
      UUID)))

(def get-map
  {:resource    :posts
   :view        views/fetch-posts
   :basic-query (fn []
                  (-> (select :*)
                      (from :posts)))
   :add-id      (fn [query id]
                  (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def put-map
  {:resource    :posts
   :view        views/create-post
   :basic-query (fn [] (insert-into :posts))
   :add-id      (fn [query _] query)
   :add-body    (fn [query form-params user-id]
                  (-> query (columns :content :user_id) (values [[(:content form-params) user-id]])))
   :over        (fn [query _ _] query)})

(def post-map
  {:resource    :posts
   :view        views/update-post
   :basic-query (fn [] (helpers/update :posts))
   :add-id      (fn [query id] (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query form-params _] (-> query (sset {:content (:content form-params)})))
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def delete-map
  {:resource    :posts
   :view        views/delete-post
   :basic-query (fn [] (delete-from :posts))
   :add-id      (fn [query id] (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def multi-get-map
  {:resource    :posts
   :view        views/all-posts
   :basic-query (fn []
                  (-> (select :*)
                      (from :posts)))
   :add-id      (fn [query _] query)
   :add-body    (fn [query form-params _]
                  (if-let [ids (:ids form-params)]
                    (-> query (where [:in :id (map #(UUID/fromString %) (if (coll? ids) ids [ids]))]))
                    query))
   :over (fn [query user-id over]
           (if (= :own over)
             (-> query (merge-where [:= :user_id user-id]))
             query))})

(def get-with-comments
  (assoc get-map :view views/fetch-post-with-comments))
