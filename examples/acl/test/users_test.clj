(ns users-test
  (:require
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
                     test_staff]]))

(use-fixtures :once acl-fixture/std-system-fixture)

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
                   :body
                   (json/read-str :key-fn keyword)
                   :data
                   :db-data
                   first
                   strip-kw)]


    (is (= expected (select-keys actual [:password
                                         :username
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
                     :body
                     (json/read-str :key-fn keyword)
                     :data
                     :db-data
                     first)
        user-id (:users/id original)
        updated (-> (post :users
                          test_admin
                          user-id
                          {:id user-id
                           :password   "not null"
                           :username   "Modified member"
                           :first_name "John"
                           :last_name  "Smith"
                           :email      "josm@test.com"
                           :is_active  true})
                    :body
                    (json/read-str :key-fn keyword)
                    :data
                    :db-data
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
                     :body
                     (json/read-str :key-fn keyword)
                     :data
                     :db-data
                     first)
        user-id (:users/id original)
        updated (-> (post :users
                          test_member
                          user-id
                          {:id         user-id
                           :password   "not null"
                           :username   "Modified member"
                           :first_name "John"
                           :last_name  "Smith"
                           :email      "josm@test.com"
                           :is_active  true})
                    :body
                    (json/read-str :key-fn keyword)
                    :data
                    :db-data)]
    (is (empty? updated) "A member cannot update another member")))
