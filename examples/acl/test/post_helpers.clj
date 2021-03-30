(ns post-helpers
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]))

(defn init-db-with-two-posts
  []
  (helpers/delete :posts)
  (helpers/put :posts "Test post")
  (helpers/put :posts "Second Test post"))

(defn post-ids
  [body]
  (map :id (-> body
               (json/read-str :key-fn keyword)
               :data
               :db-data)))

(defn all-post-ids
  []
  (-> (helpers/fetch :posts)
      :body
      post-ids))

(defn update-count
  [body]
  (-> body
      (json/read-str :key-fn clojure.core/keyword)
      (get-in [:data :db-data 0 :update-count])))
