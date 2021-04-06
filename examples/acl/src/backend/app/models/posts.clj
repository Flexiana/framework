(ns models.posts
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
                                            (from :posts))
                                  id (where [:= :id (UUID/fromString id)])))))

(defn fetch-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn add-query
  [{{{user-id :id} :user}             :session-data
    {{content :content} :body-params} :request
    :as                               state}]
  (xiana/ok (assoc state :query (-> (insert-into :posts)
                                    (values [{:content content :user_id user-id}])))))

(defn update-query
  [{{{id :id} :query-params}          :request
    {{content :content} :body-params} :request
    :as                               state}]
  (xiana/ok (assoc state :query (-> (helpers/update :posts)
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
  (xiana/ok (assoc state :query (cond-> (delete-from :posts)
                                  id (where [:= :id (UUID/fromString id)])))))

(defn delete-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn fetch-by-ids-query
  [{{{ids :ids} :body-params} :request
    :as                       state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :posts))
                                  ids (where [:in :id (map #(UUID/fromString %) (if (coll? ids) ids [ids]))])))))

(defn fetch-by-ids-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :user_id user-id]))
                                   query)))))

(defn fetch-with-comments-query
  [{{{id :id} :query-params} :request
    :as                      state}]
  (xiana/ok (assoc state :query (cond-> (-> (select :*)
                                            (from :posts)
                                            (left-join :comments [:= :posts.id :comments.post_id]))
                                  id (where [:= :posts.id (UUID/fromString id)])))))

(defn fetch-with-comments-over-fn
  [state]
  (xiana/ok (assoc state :over (fn [query user-id over]
                                 (if (= :own over)
                                   (-> query (merge-where [:= :posts.user_id user-id]))
                                   query)))))