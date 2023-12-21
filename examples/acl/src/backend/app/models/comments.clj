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
     :as helpers]))

(defn fetch-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (assoc state :query (cond->
                        (from (select :*) :comments)
                        id (where [:= :id (parse-uuid id)]))))

(defn add-query
  [{{user-id :users/id}                                :session-data
    {{content :content post-id :post_id} :body-params} :request
    :as                                                state}]
  (let [pid (try (parse-uuid post-id)
                 (catch Exception _ (random-uuid)))]
    (assoc state :query (-> (insert-into :comments)
                            (columns :content :post_id :user_id)
                            (values [[content pid user-id]])))))

(defn update-query
  [{{{id :id} :params}                :request
    {{content :content} :body-params} :request
    :as                               state}]
  (assoc state :query (-> (helpers/update :comments)
                          (where [:= :id (parse-uuid id)])
                          (sset {:content content}))))

(defn delete-query
  [{{{id :id} :params} :request
    :as                state}]
  (assoc state :query (cond->
                        (delete-from :comments)
                        id (where [:= :id (parse-uuid id)]))))
