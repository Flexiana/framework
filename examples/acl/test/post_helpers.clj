(ns post-helpers
  (:require
    [clojure.data.json :refer [read-str]]
    [helpers]
    [jsonista.core :as j]))

(defn init-db-with-two-posts
  []
  (helpers/delete :posts)
  (helpers/put :posts {:content "Test post"})
  (helpers/put :posts {:content "Second Test post"}))

(defn post-ids
  [body]
  (map :posts/id (-> body
                     (j/read-value j/keyword-keys-object-mapper)
                     :data
                     :posts)))

(defn all-post-ids
  []
  (-> (helpers/fetch :posts)
      :body
      post-ids))

(defn update-count
  [body]
  (-> body
      (j/read-value j/keyword-keys-object-mapper)
      (get-in [:data :posts])
      count))
