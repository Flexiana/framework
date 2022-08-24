(ns models.users
  (:require
    [clojure.core :as core]
    [honeysql.helpers :refer [select
                              from
                              where
                              insert-into
                              delete-from
                              merge-left-join
                              left-join
                              values
                              sset]
     :as helpers])
  (:import
    (java.util
      UUID)))

(defn ->store-user
  [m]
  (assoc
    (select-keys m [:id :password :last_login :is_superuser :username :first_name :last_name :email :is_staff :is_active])
    :is_active true
    :is_staff false
    :is_superuser false))

(defn fetch-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (assoc state :query (cond->
                        (from (select :*) :users)
                        id (where [:= :id (UUID/fromString id)]))))

(defn add-query
  [{{body :body-params} :request
    :as                 state}]
  (assoc state
         :query
         (values (insert-into :users) [(->store-user body)])))

(defn update-query
  [{{{id :id} :query-params} :request
    {body :body-params}      :request
    :as                      state}]
  (assoc state :query (-> (helpers/update :users)
                          (where [:= :id (UUID/fromString id)])
                          (sset (core/update (->store-user body) :id #(UUID/fromString %))))))

(defn delete-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (assoc state :query (cond->
                        (delete-from :users)
                        id (where [:= :id (UUID/fromString id)]))))

(defn fetch-with-post-comments-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (assoc state :query (cond-> (-> (select :*)
                                  (from :users)
                                  (left-join :posts [:= :posts.user_id :users.id])
                                  (merge-left-join :comments [:= :posts.id :comments.post_id]))
                        id (where [:= :users.id (UUID/fromString id)]))))

(defn fetch-with-post-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (assoc state :query (cond-> (-> (select :*)
                                  (from :users)
                                  (left-join :posts [:= :posts.user_id :users.id]))
                        id (where [:= :users.id (UUID/fromString id)]))))
