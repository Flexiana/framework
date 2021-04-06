(ns controllers.users
  (:require
    [clojure.core :as core]
    [honeysql.helpers :refer :all :as helpers]
    [views.users :as views]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn ->store-user
  [m]
  (let [u (select-keys m [:id
                          :password
                          :last_login
                          :is_superuser
                          :username
                          :first_name
                          :last_name
                          :email
                          :is_staff
                          :is_active])]
    (cond-> u
      (:is_active u) (assoc :is_active true)
      (:is_staff u) (assoc :is_staff true)
      (:is_superuser u) (assoc :is_superuser true)
      (nil? (:is_active u)) (assoc :is_active false)
      (nil? (:is_staff u)) (assoc :is_staff false)
      (nil? (:is_superuser u)) (assoc :is_superuser false))))

(defn fetch
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [query (cond-> (-> (select :*)
                          (from :users))
                id (where [:= :id (UUID/fromString id)]))
        view views/fetch-users]
    (xiana/ok (assoc state :query query :view view))))

(defn add
  [{{body :body-params} :request
    :as                 state}]
  (let [view views/fetch-users
        query (-> (insert-into :users)
                  (values [(->store-user body)]))]
    (xiana/ok (assoc state :view view :query query))))

(defn update-user
  [{{{id :id} :query-params} :request
    {body :body-params}      :request
    :as                      state}]
  (let [query (-> (helpers/update :users)
                  (where [:= :id (UUID/fromString id)])
                  (sset (core/update (->store-user body) :id #(UUID/fromString %))))
        view views/fetch-users
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn delete-user
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [view views/fetch-users
        query (cond-> (delete-from :users)
                id (where [:= :id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn fetch-with-posts-comments
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [view views/fetch-posts-comments
        query (cond-> (-> (select :*)
                          (from :users)
                          (left-join :posts [:= :posts.user_id :users.id])
                          (merge-left-join :comments [:= :posts.id :comments.post_id]))
                id (where [:= :users.id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :users.user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn fetch-with-posts
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [view views/fetch-posts
        query (cond-> (-> (select :*)
                          (from :users)
                          (left-join :posts [:= :posts.user_id :users.id]))
                id (where [:= :users.id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :users.user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))
