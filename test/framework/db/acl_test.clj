(ns framework.db.acl-test
  (:require
    [clojure.test :refer [is deftest]]
    [framework.db.acl :refer [->roles
                              insert-action
                              acl]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers]))

(def mock-db
  {:users             [{:id      1
                        :name    "John"
                        :surname "Doe"
                        :email   "doe.john@test.com"
                        :role    :customer}
                       {:id      2
                        :name    "Admin"
                        :surname "Doe"
                        :email   "doe.admin@test.com"
                        :role    :administrator}
                       {:id      3
                        :name    "Warehouse"
                        :surname "Boy"
                        :email   "boy@warehouse.com"
                        :role    :warehouse-worker}
                       {:id      4
                        :name    "Shop"
                        :surname "Bi"
                        :email   "bishop@warehouse.com"
                        :role    :shop-worker}
                       {:id      5
                        :name    "Postal"
                        :surname "Service"
                        :email   "postoffice@warehouse.com"
                        :role    :postal-worker}
                       {:id      6
                        :name    "Janet"
                        :surname "Doe"
                        :email   "doe.janet@test.com"
                        :role    :customer}]
   :items             [{:id    1
                        :name  "salami slicer"
                        :price {:value    28.49
                                :currency "USD"}}
                       {:id    2
                        :name  "bread knife"
                        :price {:value    12.97
                                :currency "USD"}}
                       {:id    3
                        :name  "Meat tenderizer"
                        :price {:value    14.99
                                :currency "USD"}}
                       {:id    4
                        :name  "cheese grater"
                        :price {:value    12.49
                                :currency "USD"}}]
   :addresses         [{:id          1
                        :country     "Czech Republic"
                        :city        "Prague"
                        :street      "Apolinářská"
                        :number      "447/4A"
                        :postal-code "128 00"}
                       {:id          2
                        :country     "Hungary"
                        :city        "Budapest"
                        :street      "Teréz krt."
                        :number      "33"
                        :postal-code "1083"}]
   :postal-addresses  [{:user-id    1
                        :address-id 1}
                       {:user-id    2
                        :address-id 2}]
   :invoice-addresses [{:user-id    1
                        :address-id 1}
                       {:user-id    2
                        :address-id 2}]
   :roles             [{:customer [{:table   "items"
                                    :actions [:select]
                                    :filter  :all}
                                   {:table   "users"
                                    :actions [:select :update :delete]
                                    :filter  :own}
                                   {:table   "addresses"
                                    :actions [:select :update :delete]
                                    :filter  :own}
                                   {:table   "carts"
                                    :actions [:select :update :delete]
                                    :filter  :own}]}
                       {:warehouse-worker [{:table   "items"
                                            :actions [:select :update]
                                            :filter  :all}]}
                       {:postal-worker [{:table   "carts"
                                         :actions [:select :update]
                                         :filter  :all}
                                        {:table   "addresses"
                                         :actions [:select]
                                         :filter  :all}]}
                       {:shop-worker [{:table   "items"
                                       :actions :all
                                       :filter  :all}]}
                       {:administrator [{:table   :all
                                         :actions :all
                                         :filter  :all}]}]})

(defn fetch-db
  [db table where]
  (if-let [found (filter where (get db table))]
    (if (next found) found (first found))))

(defn add-user-by-id
  [env user-id]
  (let [user (fetch-db mock-db :users #(= user-id (:id %)))
        role (:role user)]
    (cond-> env
      user (assoc-in [:session :user] user)
      role (assoc-in [:session :user :roles] (get (fetch-db mock-db :roles role) role)))))

(deftest get-roles-from-query-string
  (is (= '({:table "test_table" :actions [:select]}) (->roles "SELECT * FROM test_table")))
  (is (= '({:table "films" :actions [:insert]}) (->roles "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '({:table "producers" :actions [:select]} {:table "films" :actions [:delete]}) (->roles "DELETE FROM films\n  WHERE producer_id IN (SELECT id FROM producers WHERE name = 'foo');")))
  (is (= '({:actions [:update] :table "films"}) (->roles "UPDATE films SET kind = 'Dramatic' WHERE kind = 'Drama';")))
  (is (= '({:actions [:drop] :table "conversation"}) (->roles "DROP TABLE conversation;")))
  (is (= '({:actions [:truncate] :table "bigtable"}) (->roles "TRUNCATE bigtable;")))
  (is (= '({:actions [:alter] :table "employees"}) (->roles "ALTER TABLE employees ADD COLUMN address text")))
  (is (= '({:actions [:alter] :table "employees"}) (->roles "ALTER TABLE employees DROP COLUMN address"))))

(deftest get-roles-from-query-map
  (is (= '({:table "test_table" :actions [:select]}) (->roles (-> (select :*) (from "test_table")))))
  (is (= '({:table "films" :actions [:insert]}) (->roles (-> (insert-into "films")
                                                             (columns :code, :title, :did, :date_prod, :kind)
                                                             (values [["T_601", "Yojimbo", 106, "1961-06-16", "Drama"]])))))
  (is (= '({:table "producers" :actions [:select]} {:table "films" :actions [:delete]}) (->roles (-> (delete-from "films")
                                                                                                     (where [:in "producer_id" (-> (select :id)
                                                                                                                                   (from :producers)
                                                                                                                                   (where [:= :name "foo"]))])))))
  (is (= '({:actions [:update] :table "films"}) (->roles (-> (helpers/update :films)
                                                             (sset {:kind "Dramatic"})
                                                             (where [:= :kind "Drama"])
                                                             sql/format))))

  ;Drop isn't supported by HoneySQL
  ;(is (= '({:actions [:drop] :table "conversation"}) (->roles "DROP TABLE conversation;"))))

  (is (= '({:actions [:truncate] :table "bigtable"}) (->roles (truncate :bigtable)))))
  ;ALTER isn't supported by HoneySQL
  ;(is (= '({:actions [:alter] :table "employees"}) (->roles "ALTER TABLE employees ADD COLUMN address text")))
  ;(is (= '({:actions [:alter] :table "employees"}) (->roles "ALTER TABLE employees DROP COLUMN address"))))

(deftest insert-action-test
  (is (= [{:table   "films"
           :actions #{:select}}]
         (insert-action [] "films" :select)))
  (is (= [{:table   "films"
           :actions #{:update :select}}]
         (insert-action [{:table   "films"
                          :actions [:select]}] "films" :update)))
  (is (= [{:table   "films"
           :actions #{:update :select}}]
         (insert-action [{:table   "films"
                          :actions [:select :update]}] "films" :update)))
  (is (= [{:table   "films"
           :actions #{:select}}
          {:table   "producers"
           :actions #{:update :select}}]
         (insert-action [{:table   "producers"
                          :actions #{:update :select}}] "films" :select)))
  (is (= [{:table   "films"
           :actions #{:select}}
          {:table   "producers"
           :actions #{:update :select}}]
         (insert-action [{:table   "producers"
                          :actions #{:update :select}}] :films :select)))
  (is (= [{:table   "films"
           :actions #{:select}}
          {:table   "producers"
           :actions #{:update :select}}]
         (insert-action [{:table   "producers"
                          :actions #{:update :select}}] ["films"] :select)))
  (is (= [{:table   "films"
           :actions #{:select}}
          {:table   "producers"
           :actions #{:update :select}}]
         (insert-action [{:table   "producers"
                          :actions #{:update :select}}] [:films] :select))))

(deftest inject-user
  (is (= {:session
          {:user
           {:id      1,
            :name    "John"
            :surname "Doe"
            :email   "doe.john@test.com"
            :role    :customer
            :roles   [{:table "items", :actions [:select], :filter :all}
                      {:table "users", :actions [:select :update :delete], :filter :own}
                      {:table "addresses", :actions [:select :update :delete], :filter :own}
                      {:table "carts", :actions [:select :update :delete], :filter :own}]}}}
         (add-user-by-id {} 1)))
  (is (= {:session {:user {:id      2
                           :name    "Admin"
                           :surname "Doe"
                           :email   "doe.admin@test.com"
                           :role    :administrator
                           :roles   [{:table :all :actions :all :filter :all}]}}}
         (add-user-by-id {} 2))))

(deftest customer-on-items
  (is (true? (acl (add-user-by-id {} 1) "SELECT * FROM items;")))
  (is (false? (acl (add-user-by-id {} 1) "INSERT INTO items ;")))
  (is (false? (acl (add-user-by-id {} 1) "DELETE FROM items WHERE id EQ 125;")))
  (is (false? (acl (add-user-by-id {} 1) "UPDATE items WHERE id EQ 123;"))))

(deftest customer-on-users
  (is (true? (acl (add-user-by-id {} 1) "SELECT * FROM users WHERE id EQ 1")))
  (is (false? (acl (add-user-by-id {} 1) "SELECT * FROM users;")))
  (is (false? (acl (add-user-by-id {} 1) "SELECT * FROM users WHERE user-id EQ 2")))
  (is (false? (acl (add-user-by-id {} 1) "INSERT INTO users")))
  (is (true? (acl (add-user-by-id {} 1) "DELETE FROM users WHERE id EQ 1")))
  (is (false? (acl (add-user-by-id {} 1) "DELETE FROM users WHERE user-id EQ 2")))
  (is (false? (acl (add-user-by-id {} 1) "DELETE FROM users WHERE id EQ 2")))
  (is (true? (acl (add-user-by-id {} 1) "UPDATE users WHERE user-id EQ 1")))
  (is (false? (acl (add-user-by-id {} 1) "UPDATE users WHERE user-id EQ 2"))))

(deftest user-addresses
  (is (true? (acl (add-user-by-id {} 1)
                  "SELECT * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE user.id EQ 1")))
  (is (false? (acl (add-user-by-id {} 1)
                   "SELECT * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE user.id EQ 2")))
  (is (true? (acl (add-user-by-id {} 1)
                  "UPDATE addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE user.id EQ 1")))
  (is (false? (acl (add-user-by-id {} 1)
                   "UPDATE * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE user.id EQ 2")))
  (is (true? (acl (add-user-by-id {} 1)
                  "DELETE FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE user.id EQ 1")))
  (is (false? (acl (add-user-by-id {} 1)
                   "DELETE FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE user.id EQ 2"))))


;(is (false? (acl (add-user-by-id {} 1) "SELECT * FROM addresses WHERE id EQ 1")))
;(is (false? (acl (add-user-by-id {} 1) "SELECT * FROM addresses;")))
;(is (true? (acl (add-user-by-id {} 1) "UPDATE addresses WHERE user-id EQ 1;")))
;(is (false? (acl (add-user-by-id {} 1) "UPDATE addresses WHERE user-id EQ 2;")))
;(is (false? (acl (add-user-by-id {} 1) "UPDATE addresses;")))

;(def sqlmap
;  {:select [:a :b :c]
;   :from   [:users]
;   :where  [:= :id "baz"]})
;
;(sql/format sqlmap)

