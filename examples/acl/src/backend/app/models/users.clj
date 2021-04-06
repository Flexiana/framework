(ns models.users
  (:require
    [clojure.core :as core]
    [honeysql.helpers :refer :all :as helpers]
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

(defn fetch-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :users))
                                  id (where [:= :id (UUID/fromString id)])))))

(defn add-query
  [{{body :body-params} :request
    :as                 state}]
  (xiana/ok (assoc state :query (-> (insert-into :users)
                                    (values [(->store-user body)])))))

(defn update-query
  [{{{id :id} :query-params} :request
    {body :body-params}      :request
    :as                      state}]
  (xiana/ok (assoc state :query (-> (helpers/update :users)
                                    (where [:= :id (UUID/fromString id)])
                                    (sset (core/update (->store-user body) :id #(UUID/fromString %)))))))

(defn update-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :id user-id]))
                                   query)))))

(defn delete-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (delete-from :users)
                                  id (where [:= :id (UUID/fromString id)])))))

(defn delete-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn fetch-with-post-comments-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :users)
                                            (left-join :posts [:= :posts.user_id :users.id])
                                            (merge-left-join :comments [:= :posts.id :comments.post_id]))
                                  id (where [:= :users.id (UUID/fromString id)])))))

(defn fetch-with-post-comments-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :users.user_id user-id]))
                                   query)))))

(defn fetch-with-post-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :users)
                                            (left-join :posts [:= :posts.user_id :users.id]))
                                  id (where [:= :users.id (UUID/fromString id)])))))

(defn fetch-with-post-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :users.user_id user-id]))
                                   query)))))
