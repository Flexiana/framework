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
    (let [new-posts (-> (helpers/fetch "posts/comments" test_admin)
                        :body
                        (json/read-str :key-fn keyword)
                        :data
                        :posts)]
      (is (= 1 (->> (filter #(#{first-id} (:posts/id %)) new-posts)
                    first
                    :comments
                    count)))
      (is (= 0 (->> (remove #(#{first-id} (:posts/id %)) new-posts)
                    first
                    :comments
                    count))))))

(defn ->comments
  [response]
  (-> response
      :body
      (json/read-str :key-fn keyword)
      :data
      :comments))

(deftest update-comment
  (init-db-with-two-posts)
  (let [post-ids (all-post-ids)
        first-id (first post-ids)
        comment (-> (helpers/put :comments test_member {:post_id first-id
                                                        :content "Comment to update"})
                    ->comments
                    first)
        new-comment (-> (helpers/post :comments test_member (:comments/id comment) {:content "Updated comment"})
                        ->comments
                        first)]
    (is (= "Updated comment" (:comments/content new-comment)))))

(deftest delete-comment
  (init-db-with-two-posts)
  (let [post-ids (all-post-ids)
        first-id (first post-ids)
        comment (-> (helpers/put :comments test_member {:post_id first-id
                                                        :content "Comment to update"})
                    ->comments
                    first)
        comment-id (:comments/id comment)
        deleted (-> (helpers/delete :comments test_member comment-id)
                    ->comments
                    first)]
    (is (= comment deleted))
    (is (empty? (-> (helpers/fetch :comments test_member comment-id)
                    ->comments)))))
