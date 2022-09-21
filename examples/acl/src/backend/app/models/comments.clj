(ns models.comments
  (:require
    [honeysql.helpers :refer [select
                              from
                              where
                              insert-into
                              delete-from
                              columns
                              values
                              sset]
     :as helpers])
  (:import
    (java.util
      UUID)))

(defn fetch-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (assoc state :query (cond->
                        (from (select :*) :comments)
                        id (where [:= :id (UUID/fromString id)]))))

(defn add-query
  [{{user-id :users/id}                                :session-data
    {{content :content post-id :post_id} :body-params} :request
    :as                                                state}]
  (let [pid (try (UUID/fromString post-id)
                 (catch Exception _ (UUID/randomUUID)))]
    (assoc state :query (-> (insert-into :comments)
                            (columns :content :post_id :user_id)
                            (values [[content pid user-id]])))))

(defn update-query
  [{{{id :id} :params}                :request
    {{content :content} :body-params} :request
    :as                               state}]
  (assoc state :query (-> (helpers/update :comments)
                          (where [:= :id (UUID/fromString id)])
                          (sset {:content content}))))

(defn delete-query
  [{{{id :id} :params} :request
    :as                state}]
  (assoc state :query (cond->
                        (delete-from :comments)
                        id (where [:= :id (UUID/fromString id)]))))
