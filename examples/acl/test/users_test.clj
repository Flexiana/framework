(ns users-test
  (:require
    [acl-fixture]
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [helpers :refer [delete
                     put
                     fetch
                     post
                     test_member
                     test_admin]]
    [post-helpers]))

(use-fixtures :once acl-fixture/std-system-fixture)

(defn ->users
  [response]
  (-> response
      :body
      (json/read-str :key-fn keyword)
      :data
      :users))

(defn strip-kw
  [m]
  (into {} (map (fn [[k v]] [(keyword (name k)) v]) m)))

(deftest create-user
  (let [expected {:password   "not null"
                  :username   "Additional member"
                  :first_name "John"
                  :last_name  "Smith"
                  :email      "josm@test.com"
                  :is_active  true}
        actual (-> (put :users
                        test_admin
                        expected)
                   ->users
                   first
                   strip-kw)]
    (is (= (select-keys expected [:username
                                  :first_name
                                  :last_name
                                  :email
                                  :is_active])
           (select-keys actual [:username
                                :first_name
                                :last_name
                                :email
                                :is_active])))))

(deftest update-user
  (let [original (-> (put :users
                          test_admin
                          {:password   "not null"
                           :username   "Additional member"
                           :first_name "John"
                           :last_name  "Smith"
                           :email      "josm@test.com"
                           :is_active  true})
                     ->users
                     first)
        user-id (:users/id original)
        updated (-> (post :users
                          test_admin
                          user-id
                          {:id         user-id
                           :password   "not null"
                           :username   "Modified member"
                           :first_name "John"
                           :last_name  "Smith"
                           :email      "josm@test.com"
                           :is_active  true})
                    ->users
                    first)]
    (is (= "Modified member" (:users/username updated)) "Admin is able to update a member"))
  (let [original (-> (put :users
                          test_admin
                          {:password   "not null"
                           :username   "Additional member"
                           :first_name "John"
                           :last_name  "Smith"
                           :email      "josm@test.com"
                           :is_active  true})
                     ->users
                     first)
        user-id (:users/id original)
        updated (->users (post :users test_member user-id {:email "josm@test.com", :first_name "John", :password "not null", :is_active true, :username "Modified member", :id user-id, :last_name "Smith"}))]
    (is (empty? updated) "A member cannot update another member")))

(deftest get-user-posts-with-comment
  (delete :posts)
  (let [post-id (-> (put :posts test_member {:content "This is a post"})
                    :body
                    post-helpers/post-ids
                    first)
        _ (put :comments test_member {:post_id post-id :content "Test comment on test post"})
        result (-> (fetch "users/posts/comments" test_member test_member)
                   ->users
                   first)]
    (is (= "This is a post" (get-in result [:posts 0 :posts/content])))
    (is (= "Test comment on test post" (get-in result [:posts 0 :comments 0 :comments/content])))))

(deftest get-user-posts
  (delete :posts)
  (let [post-id (-> (put :posts test_member {:content "This is a test post"})
                    :body
                    post-helpers/post-ids
                    first)
        _ (put :comments test_member {:post_id post-id :content "Test comment on test post"})
        result (-> (fetch "users/posts" test_member test_member)
                   ->users
                   first)]
    (is (= "This is a test post" (get-in result [:posts 0 :posts/content])))
    (is (nil? (get-in result [:posts 0 :comments 0 :comments/content])))))

(deftest member-cannot-delete-others
  (let [new-user-id (-> (put :users
                             test_admin
                             {:password   "not null"
                              :username   "Additional member"
                              :first_name "John"
                              :last_name  "Smith"
                              :email      "josm@test.com"
                              :is_active  true})
                        ->users
                        first
                        :users/id)]
    (is (empty? (->users (delete :users test_member new-user-id))))))

(deftest member-can-delete-itself
  (let [new-user-id (-> (put :users
                             test_admin
                             {:password   "not null"
                              :username   "Additional member"
                              :first_name "John"
                              :last_name  "Smith"
                              :email      "josm@test.com"
                              :is_active  true})
                        ->users
                        first
                        :users/id)]
    (is (= new-user-id (-> (delete :users new-user-id new-user-id)
                           ->users
                           first
                           :users/id)))))

(deftest admin-can-delete-others
  (let [new-user-id (-> (put :users
                             test_admin
                             {:password   "not null"
                              :username   "Additional member"
                              :first_name "John"
                              :last_name  "Smith"
                              :email      "josm@test.com"
                              :is_active  true})
                        ->users
                        first
                        :users/id)]
    (is (= new-user-id (-> (delete :users test_admin new-user-id)
                           ->users
                           first
                           :users/id)))))

(deftest admin-can-delete-itself
  (let [new-user-id (-> (put :users
                             test_admin
                             {:password     "not null"
                              :username     "Additional member"
                              :first_name   "John"
                              :last_name    "Smith"
                              :email        "josm@test.com"
                              :is_superuser true
                              :is_active    true})
                        ->users
                        first
                        :users/id)]
    (is (= new-user-id (-> (delete :users new-user-id new-user-id)
                           ->users
                           first
                           :users/id)))))
