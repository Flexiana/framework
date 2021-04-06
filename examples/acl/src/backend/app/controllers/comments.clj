(ns controllers.comments
  (:require
    [honeysql.helpers :refer :all :as helpers]
    [views.comments :as views]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn fetch
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [query (cond-> (-> (select :*)
                          (from :comments))
                      id (where [:= :id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))
        view views/all-comments]
    (xiana/ok (assoc state :query query :over over-fn :view view))))

(defn add
  [{{{user-id :id} :user}                              :session-data
    {{content :content post-id :post_id} :body-params} :request
    :as                                                state}]
  (let [view views/all-comments
        query (-> (insert-into :comments)
                  (columns :content :post_id :user_id)
                  (values [[content (UUID/fromString post-id) user-id]]))]
    (xiana/ok (assoc state :view view :query query))))

(defn update-comment
  [{{{id :id} :query-params}          :request
    {{content :content} :body-params} :request
    :as                               state}]
  (let [query (-> (helpers/update :comments)
                  (where [:= :id (UUID/fromString id)])
                  (sset {:content content}))
        view views/all-comments
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))

(defn delete-comment
  [{{{id :id} :query-params} :request
    :as                      state}]
  (let [view views/all-comments
        query (cond-> (delete-from :comments)
                      id (where [:= :id (UUID/fromString id)]))
        over-fn (fn [query user-id over]
                  (if (= :own over)
                    (-> query (merge-where [:= :user_id user-id]))
                    query))]
    (xiana/ok (assoc state :query query :view view :over over-fn))))


