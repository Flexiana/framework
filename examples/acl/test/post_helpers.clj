(ns post-helpers
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]))

(defn init-db-with-two-posts
  []
  (helpers/delete :posts)
  (helpers/put :posts {:content "Test post"})
  (helpers/put :posts {:content "Second Test post"}))

(defn post-ids
  [body]
  (map :id (-> body
               (json/read-str :key-fn keyword)
               :data
               :db-data
               :posts)))

(defn all-post-ids
  []
  (-> (helpers/fetch :posts)
      :body
      post-ids))

(defn update-count
  [body]
  (-> body
      (json/read-str :key-fn clojure.core/keyword)
      (get-in [:data :db-data :posts])
      count))
