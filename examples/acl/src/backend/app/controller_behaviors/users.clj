(ns controller-behaviors.users
  (:require
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]
    [views.users :as views])
  (:import
    (java.util
      UUID)))

(defn select-non-nil-keys
  [m v]
  (reduce (fn [acc k]
            (if (nil? (k m)) acc (assoc acc k (k m)))) {} v))

(defn ->store-user
  [m]
  (select-keys m [:id
                  :password
                  :last_login
                  :is_superuser
                  :username
                  :first_name
                  :last_name
                  :email
                  :is_staff
                  :is_active]))

(def get-map
  {:resource    :users
   :view        views/fetch-posts
   :on-deny     views/not-allowed
   :basic-query (fn []
                  (-> (select :*)
                      (from :users)))
   :add-id      (fn [query id]
                  (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query _ _] query)})

(def put-map
  {:resource    :users
   :view        views/create-post
   :on-deny     views/not-allowed
   :basic-query (fn [] (insert-into :users))
   :add-id      (fn [query _] query)
   :add-body    (fn [query form-params _]
                  (-> query (values [(->store-user form-params)])))
   :over        (fn [query _ _] query)})

(def post-map
  {:resource    :users
   :view        views/update-post
   :on-deny     views/not-allowed
   :basic-query (fn [] (helpers/update :users))
   :add-id      (fn [query id] (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query form-params _] (-> query (sset (->store-user form-params))))
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def delete-map
  {:resource    :users
   :view        views/delete-post
   :on-deny     views/not-allowed
   :basic-query (fn [] (delete-from :users))
   :add-id      (fn [query id] (-> query (where [:= :id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def multi-get-map
  {:resource    :users
   :view        views/all-posts
   :on-deny     views/not-allowed
   :basic-query (fn []
                  (-> (select :*)
                      (from :users)))
   :add-id      (fn [query _] query)
   :add-body    (fn [query form-params _]
                  (if-let [ids (:ids form-params)]
                    (-> query (where [:in :id (map #(UUID/fromString %) (if (coll? ids) ids [ids]))]))
                    query))
   :over        (fn [query _ _] query)})

(def fetch-posts
  {:resource    :users
   :view        views/fetch-posts
   :on-deny     views/not-allowed
   :basic-query (fn []
                  (-> (select :*)
                      (from :users)
                      (left-join :posts [:= :users.id :posts.user_id])))
   :add-id      (fn [query id]
                  (-> query (where [:= :users.id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})

(def fetch-posts-comments
  {:resource    :users
   :view        views/fetch-posts
   :on-deny     views/not-allowed
   :basic-query (fn []
                  (-> (select :*)
                      (from :users)
                      (left-join :posts [:= :users.id :posts.user_id])
                      (merge-left-join :comments [:= :comments/post_id :posts.id])))
   :add-id      (fn [query id]
                  (-> query (where [:= :users.id (UUID/fromString id)])))
   :add-body    (fn [query _ _] query)
   :over        (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))})
