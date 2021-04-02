(ns comments-test
  (:require
    [acl]
    [acl-fixture]
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [helpers :refer [delete
                     put
                     fetch
                     post
                     test_member
                     test_admin
                     test_suspended_admin
                     test_staff]]
    [post-helpers :refer [init-db-with-two-posts
                          all-post-ids]]))

(use-fixtures :once acl-fixture/std-system-fixture)

(deftest commenting-on-post
  (init-db-with-two-posts)
  (let [post-ids (all-post-ids)
        first-id (first post-ids)]
    (helpers/put :comments test_admin {:post_id first-id
                                       :content "Test comment on first post"})
    (let [new-posts (-> (helpers/fetch "posts/comments" test_admin first-id)
                        :body
                        (json/read-str :key-fn keyword)
                        :data
                        :posts)]
      (->> (filter #(#{first-id} (:posts/id %)) new-posts)
           first
           :comments
           count
           (= 1)
           is)
      (->> (remove #(#{first-id} (:posts/id %)) new-posts)
           first
           :comments
           count
           (= 0)
           is))))

