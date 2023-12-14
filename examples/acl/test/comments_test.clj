(ns comments-test
  (:require
    [acl]
    [acl-fixture :refer [std-system-fixture]]
    [clojure.data.json :refer [read-str]]
    [clojure.test :refer [deftest is use-fixtures]]
    [helpers :refer [test_member
                     test_admin]]
    [jsonista.core :as j]
    [post-helpers :refer [init-db-with-two-posts
                          all-post-ids]]))

(use-fixtures :once std-system-fixture)

(deftest commenting-on-post
  (init-db-with-two-posts)
  (let [post-ids (all-post-ids)
        first-id (first post-ids)]
    (helpers/put :comments test_admin {:post_id first-id
                                       :content "Test comment on first post"})
    (let [new-posts (-> (helpers/fetch "posts/comments" test_admin)
                        :body
                        (j/read-value j/keyword-keys-object-mapper)
                        :data
                        :posts)]
      (is (= 1 (->> (filter #(#{first-id} (:posts/id %)) new-posts)
                    first
                    :comments
                    count)))
      (is (zero? (->>
                   (remove (fn* [p1__602731#] (#{first-id} (:posts/id p1__602731#))) new-posts)
                   first
                   :comments
                   count))))))

(defn ->comments
  [response]
  (-> response
      :body
      (j/read-value j/keyword-keys-object-mapper)
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
    (is (empty? (->comments (helpers/fetch :comments test_member comment-id))))))
