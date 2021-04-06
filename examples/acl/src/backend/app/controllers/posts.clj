(ns controllers.posts
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [views.posts :as views]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn fetch
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [query (cond-> (-> (select :*)
                          (from :posts))
                id (where [:= :id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))
        view views/fetch-posts]
    (xiana/ok (assoc state :query query :over over-fn :view view))))

(defn add
  [{{{user-id :id} :user}             :session-data
    {{content :content} :body-params} :request
    :as                               state}]
  (:pre [(string? content)])
  (let [view views/update-post
        query (-> (insert-into :posts)
                  (columns :content :user_id) (values [[content user-id]]))]
    (xiana/ok (assoc state :view view :query query))))

(defn update-post
  [{{{id :id} :query-params}          :request
    {{content :content} :body-params} :request
    :as                               state}]
  (let [query (-> (helpers/update :posts)
                  (where [:= :id (UUID/fromString id)])
                  (sset {:content content}))
        view views/update-post
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn delete-post
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [view views/delete-post
        query (cond-> (delete-from :posts)
                id (where [:= :id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn fetch-by-ids
  [{{{ids :ids} :body-params} :request
    :as                       state}]
  (let [view views/all-posts
        query (cond-> (-> (select :*)
                          (from :posts))
                ids (where [:in :id (map #(UUID/fromString %) (if (coll? ids) ids [ids]))]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn fetch-with-comments
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [view views/fetch-post-with-comments
        query (cond-> (-> (select :*)
                          (from :posts)
                          (left-join :comments [:= :posts.id :comments.post_id]))
                id (where [:= :posts.id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :posts.user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))
