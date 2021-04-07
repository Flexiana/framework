(ns posts-test
  (:require
    [acl]
    [acl-fixture]
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [helpers :refer [delete
                     put
                     fetch
                     post
                     test_member
                     test_admin
                     test_suspended_admin
                     test_staff]]
    [post-helpers :refer [post-ids
                          update-count
                          init-db-with-two-posts
                          all-post-ids]]))

(use-fixtures :once acl-fixture/std-system-fixture)

(deftest guest-can-read-posts
  (init-db-with-two-posts)
  (let [orig-ids (all-post-ids)]
    (is (= [(count orig-ids) orig-ids]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :method               :get}
               http/request
               :body
               post-ids
               ((juxt count identity)))) "Guest can read all posts")
    (is (= [1 (first orig-ids)]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :query-params         {:id (first orig-ids)}
                :method               :get}
               http/request
               :body
               post-ids
               ((juxt count first)))) "Guest can read post by id")))

(deftest guest-cannot-delete-posts :posts
  (init-db-with-two-posts)
  (let [orig-ids (all-post-ids)]
    (is (= [401 "You don't have rights to do this"]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :method               :delete}
               http/request
               ((juxt :status :body)))) "Guest cannot delete all posts")
    (is (= [401 "You don't have rights to do this"]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :query-params         {:id (first orig-ids)}
                :method               :delete}
               http/request
               ((juxt :status :body)))) "Guest cannot delete post by id")))

(deftest guest-cannot-create-post
  (init-db-with-two-posts)
  (is (= [401 "You don't have rights to do this"]
         (-> {:url                  "http://localhost:3000/posts"
              :unexceptional-status (constantly true)
              :form-params          {:content "It doesn't save anyway"}
              :method               :put}
             http/request
             ((juxt :status :body)))) "Guest cannot create new post"))

(deftest guest-cannot-update-post
  (init-db-with-two-posts)
  (let [orig-ids (all-post-ids)]
    (is (= [401 "You don't have rights to do this"]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :query-params         {:id (first orig-ids)}
                :form-params          {:content "It doesn't save anyway"}
                :method               :post}
               http/request
               ((juxt :status :body)))) "Guest cannot update post")))

(deftest member-can-read-posts
  (init-db-with-two-posts)
  (let [orig-ids (all-post-ids)]
    (is (= [(count orig-ids) orig-ids] (-> (fetch :posts test_member)
                                           :body
                                           post-ids
                                           ((juxt count identity)))) "Member can read all posts")
    (is (= [1 (first orig-ids)] (-> (fetch :posts test_member (first orig-ids))
                                    :body
                                    post-ids
                                    ((juxt count first)))) "Member can read all posts")))

(deftest member-can-create-post
  (init-db-with-two-posts)
  (let [orig-ids (all-post-ids)]
    (is (= 1 (-> (put :posts test_member {:content "It will be stored"})
                 :body
                 update-count)) "Member can create post")
    (is (= (inc (count orig-ids)) (count (all-post-ids))))))

(deftest member-can-delete-own-post
  (delete :posts)
  (is (= 1 (-> (put :posts test_member {:content "Something to delete"})
               :body
               update-count)))
  (is (= 1 (-> (delete :posts test_member (first (all-post-ids)))
               :body
               update-count)))
  (is (empty? (all-post-ids))))

(deftest member-cannot-delete-others-post
  (delete :posts)
  (is (= 1 (-> (put :posts test_staff {:content "Something to delete"})
               :body
               update-count)))
  (is (= 0 (-> (delete :posts test_member (first (all-post-ids)))
               :body
               update-count)))
  (is (= 1 (count (all-post-ids)))))

(deftest member-can-update-own-post
  (delete :posts)
  (is (= 1 (-> (put :posts test_member {:content "Something to delete"})
               :body
               update-count)))
  (is (= 1 (-> (post :posts test_member (first (all-post-ids)) {:content "Or update instead"})
               :body
               update-count)))
  (is (= 1 (count (all-post-ids))))
  (is (.contains (:body (fetch :posts)) "Or update instead")))

(deftest member-cannot-update-others-post
  (delete :posts)
  (is (= 1 (-> (put :posts test_staff {:content "Something to delete"})
               :body
               update-count)))
  (is (= 0 (-> (post :posts test_member (first (all-post-ids)) {:content "Or update instead"})
               :body
               update-count)))
  (is (= 1 (count (all-post-ids))))
  (is (.contains (:body (fetch :posts)) "Something to delete")))

(deftest member-can-read-multiple-posts-by-id
  (init-db-with-two-posts)
  (put :posts {:content "Third test post"})
  (put :posts {:content "Fourth test post"})
  (let [ids (all-post-ids)]
    (is (= (dec (count ids))
           (-> {:url                  "http://localhost:3000/posts/ids"
                :headers              {"Authorization" test_admin
                                       "Content-Type" "application/json;charset=utf-8"}
                :unexceptional-status (constantly true)
                :body                 (json/write-str {:ids (butlast ids)})
                :method               :post}
               http/request
               :body
               post-ids
               count)))))

(deftest get-post-with-comments
  (delete :posts)
  (let [post-id (-> (put :posts test_member {:content "test post"})
                    :body
                    post-ids
                    first)
        comment (put :comments test_member {:post_id post-id
                                            :content "test comment on test post"})
        result (-> (fetch "posts/comments" test_member)
                   :body
                   (json/read-str :key-fn keyword)
                   :data
                   :posts
                   first)]
    (is (= "test post" (:posts/content result)))
    (is (= "test comment on test post" (get-in result [:comments 0 :comments/content])))))
