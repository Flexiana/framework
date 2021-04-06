(ns models.comments
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn fetch-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :comments))
                                  id (where [:= :id (UUID/fromString id)])))))

(defn fetch-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn add-query
  [{{{user-id :id} :user}                              :session-data
    {{content :content post-id :post_id} :body-params} :request
    :as                                                state}]
  (xiana/ok (assoc state :query (-> (insert-into :comments)
                                    (columns :content :post_id :user_id)
                                    (values [[content (UUID/fromString post-id) user-id]])))))

(defn update-query
  [{{{id :id} :query-params}          :request
    {{content :content} :body-params} :request
    :as                               state}]
  (xiana/ok (assoc state :query (-> (helpers/update :comments)
                                    (where [:= :id (UUID/fromString id)])
                                    (sset {:content content})))))

(defn update-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn delete-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (delete-from :comments)
                                  id (where [:= :id (UUID/fromString id)])))))

(defn delete-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))
