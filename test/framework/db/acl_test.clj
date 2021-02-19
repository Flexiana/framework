(ns framework.db.acl-test
  (:require
    [clojure.test :refer [is deftest]]
    [framework.db.acl :refer [->roles
                              map->owns?
                              map->where-collect
                              insert-action
                              owns?
                              acl
                              table-aliases
                              column-aliases]]
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
    (if (next found) found (first found))
    []))

(defn add-user-by-id
  [env user-id]
  (let [user (fetch-db mock-db :users #(= user-id (:id %)))
        role (:role user)]
    (cond-> env
      user (assoc-in [:session :user] user)
      role (assoc-in [:session :user :roles] (get (fetch-db mock-db :roles role) role)))))

(deftest get-table-aliases
  (is (= {"u" "users"} (table-aliases {:select '(:*) :from '([:users :u]) :where [:and [:< :id 1] [:> :id 1]]})))
  (is (= {"users" "users"} (table-aliases {:select '(:*) :from '(:users) :where [:and [:< :id 1] [:> :id 1]]})))
  (is (= {"users" "users" "c" "cart"} (table-aliases (-> (select :*)
                                                         (from :users)
                                                         (join [:cart :c] [:= :users.id :c.user-id])))))
  (is (= {"users" "users", "c" "cart"} (table-aliases (-> (select :*)
                                                          (from "users")
                                                          (join [:cart :c] [:= :users.id :c.user-id])))))
  (is (= {"users" "users", "c" "cart"} (table-aliases (-> (select :*)
                                                          (from :users)
                                                          (join [:cart :c] [:= :users.id :c.user-id]))))))

(table-aliases (-> (select :*)
                   (from :addresses)
                   (merge-from :a)
                   (join [:postal-addresses :pa] [:= :addresses.id :pa.address-id])
                   (merge-join :users [:= :users.id :pa.user-id])
                   (where [:= :users.id 1])))

(map->where-collect (-> (select :*)
                        (from :addresses)
                        (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                        (merge-join :users [:= :users.id :postal-addresses.user-id])
                        (where [:= :users.id 1])
                        (merge-where [:= :addresses.id 2])))

(deftest get-column-aliases
  (is (= {"a" "a", "bar" "b", "c" "c", "x" "d"} (column-aliases {:select '(:a [:b :bar] :c [:d :x]), :from '([:foo :quux]), :where [:and [:= :quux.a :a] [:< :bar 100]]}))))

(deftest testing-collect-actions
  (is (= '({:op :=, "foo.a" 1} {:op :<, "foo.b" 100})
         (map->where-collect {:select '(:a [:b :bar] :c [:d :x]), :from '([:foo :quux]), :where [:and [:= :quux.a 1] [:< :bar 100]]})))
  (is (true? (map->owns? (-> (select :*)
                             (from :users)
                             (where [:= :users.id 1])) 1)))
  (is (= '({:op :=, "foo.a" "bort"} {:op :not=, "baz.baz" #sql/param :param1})
         (map->where-collect (-> (select :f.* :b.baz :c.quux [:b.bla "bla-bla"]
                                         (sql/call :now) (sql/raw "@x := 10"))
                                 (modifiers :distinct)
                                 (from [:foo :f] [:baz :b])
                                 (join :draq [:= :f.b :draq.x])
                                 (left-join [:clod :c] [:= :f.a :c.d])
                                 (right-join :bock [:= :bock.z :c.e])
                                 (where [:or
                                         [:and [:= :f.a "bort"] [:not= :b.baz (sql/param :param1)]]
                                         [:< 1 2 3]
                                         [:in :f.e [1 (sql/param :param2) 3]]
                                         [:between :f.e 10 20]])
                                 (group :f.a :c.e)
                                 (having [:< 0 :f.e])
                                 (order-by [:b.baz :desc] :c.quux [:f.a :nulls-first])
                                 (limit 50)
                                 (offset 10))))))

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
                                                                                                                                   (from "producers")
                                                                                                                                   (where [:= :name "foo"]))])))))
  (is (= '({:actions [:update] :table "films"}) (->roles (-> (helpers/update :films)
                                                             (sset {:kind "Dramatic"})
                                                             (where [:= :kind "Drama"])
                                                             sql/format))))
  (is (= '({:actions [:truncate] :table "bigtable"}) (->roles (truncate :bigtable))))
  ;Drop isn't supported by HoneySQL
  (is (= [""] (sql/format {:drop "conversation"})))
  ;ALTER isn't supported by HoneySQL "ALTER TABLE employees ADD COLUMN address text"
  (is (= [" "] (sql/format {:alter-table :employees
                            :add-column  [:address :text]}))))

(deftest owner-test
  (is (true? (owns? "SELECT * FROM users WHERE id = 1" 1)))
  (is (true? (owns? (-> (select :*)
                        (from [:users :u])
                        (where [:= :id 1])) 1)))
  (is (false? (owns? (-> (select :*)
                         (from [:users :u])
                         (where [:< :id 1])
                         (merge-where [:> :id 1])) 1))))

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
                          :actions #{:update :select}}] [:films] :select)))
  (is (= [{:table   "films"
           :actions #{:select}}
          {:table   "producers"
           :actions #{:update :select}}]
         (insert-action [{:table   "producers"
                          :actions #{:update :select}}] [:films :f] :select))))

(def customer (add-user-by-id {} 1))
(def administrator (add-user-by-id {} 2))

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
         customer))
  (is (= {:session {:user {:id      2
                           :name    "Admin"
                           :surname "Doe"
                           :email   "doe.admin@test.com"
                           :role    :administrator
                           :roles   [{:table :all :actions :all :filter :all}]}}}
         administrator)))

(deftest customer-on-items
  (is (true? (acl customer "SELECT * FROM items;")))
  (is (true? (acl customer (-> (select [:*])
                               (from :items)))))
  (is (false? (acl customer "INSERT INTO items ;")))
  (is (false? (acl customer (insert-into "items"))))
  (is (false? (acl customer "DELETE FROM items WHERE id EQ 125;")))
  (is (false? (acl customer (-> (delete-from "items")
                                (where [:= :id 125])))))
  (is (false? (acl customer "UPDATE items WHERE id EQ 123;")))
  (is (false? (acl customer (-> (helpers/update "items")
                                (where [:= :id 123]))))))

(deftest customer-on-users
  (is (true? (acl customer "SELECT * FROM users WHERE id EQ 1")))
  (is (true? (acl customer (-> (select :*)
                               (from [:users :u])
                               (where [:= :u.id 1])))))
  (is (false? (acl customer "SELECT * FROM users;")))
  (is (false? (acl customer (-> (select :*)
                                (from [:users :u])))))
  (is (false? (acl customer "SELECT * FROM users WHERE user-id EQ 2")))
  (is (false? (acl customer (-> (select :*)
                                (from [:users :u])
                                (where [:= :u.id 2])))))
  (is (false? (acl customer "INSERT INTO users")))
  (is (false? (acl customer (insert-into "users"))))
  (is (true? (acl customer "DELETE FROM users WHERE id EQ 1")))
  (is (true? (acl customer (-> (delete-from :users)
                               (where [:= :id 1])))))
  (is (false? (acl customer "DELETE FROM users WHERE users.id EQ 2")))
  (is (false? (acl customer (-> (delete-from :users)
                                (where [:= :users.id 2])))))
  (is (false? (acl customer "DELETE FROM users WHERE id EQ 2")))
  (is (false? (acl customer (-> (delete-from :users)
                                (where [:= :id 2])))))
  (is (true? (acl customer "UPDATE users WHERE user-id EQ 1")))
  (is (true? (acl customer (-> (helpers/update :users)
                               (where [:= :id 1])))))
  (is (false? (acl customer "UPDATE users WHERE user-id EQ 2")))
  (is (false? (acl customer (-> (helpers/update :users)
                                (where [:= :id 2]))))))

(deftest customer-on-addresses
  ;TODO text based validation can be fooled, these should be 'false'
  (is (true? (acl customer "SELECT * FROM addresses WHERE id EQ 1")))
  (is (true? (acl customer "UPDATE addresses WHERE user-id EQ 1;")))
  ;but the same query with maps shows the real result
  (is (false? (acl customer
                   ;"SELECT * FROM addresses WHERE id EQ 1"
                   (-> (select :*)
                       (from :addresses)
                       (where [:= :id 1])))))
  ;user-id is not guaranteed to be the same as the user's id
  (is (false? (acl customer
                   ;"UPDATE addresses WHERE user-id EQ 1;"
                   (-> (helpers/update :*)
                       (from :addresses)
                       (where [:= :user-id 1])))))
  ;but mostly the results are the same
  (is (= false
         (acl customer
              "SELECT * FROM addresses;")
         ;You can use table names as keyword
         (acl customer
              (-> (select :*)
                  (from :addresses)))
         ;or string:
         (acl customer
              (-> (select :*)
                  (from "addresses")))
         ;cannot fool with aliases
         (acl customer
              (-> (select :*)
                  (from [:addresses :users])))))
  (is (true? (acl customer
                  "SELECT * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 1")))
  (is (true? (acl customer
                  (-> (select :*)
                      (from :addresses)
                      (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                      (merge-join :users [:= :users.id :postal-addresses.user-id])
                      (where [:= :users.id 1])))))
  (is (false? (acl customer
                   "SELECT * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 2")))
  (is (false? (acl customer
                   (-> (select :*)
                       (from :addresses)
                       (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                       (merge-join :users [:= :users.id :postal-addresses.user-id])
                       (where [:= :users.id 2])))))
  (is (true? (acl customer
                  "UPDATE addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 1")))
  (is (true? (acl customer
                  (-> (helpers/update :addresses)
                      (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                      (merge-join :users [:= :users.id :postal-addresses.user-id])
                      (where [:= :users.id 1])))))
  (is (false? (acl customer
                   "UPDATE * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 2")))
  (is (false? (acl customer
                   (-> (helpers/update :addresses)
                       (join [:postal-addresses :p] [:= :addresses.id :p.address-id])
                       (merge-join :users [:= :users.id :p.user-id])
                       (where [:= :users.id 2])))))
  (is (true? (acl customer
                  "DELETE FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 1")))
  (is (true? (acl customer
                  (-> (delete-from :addresses)
                      (join [:postal-addresses :p] [:= :addresses.id :p.address-id])
                      (merge-join :users [:= :users.id :p.user-id])
                      (where [:= :users.id 1])))))
  (is (false? (acl customer
                   "DELETE FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 2")))
  (is (false? (acl customer
                   (-> (delete-from :addresses)
                       (join [:postal-addresses :p] [:= :addresses.id :p.address-id])
                       (merge-join :users [:= :users.id :p.user-id])
                       (where [:= :users.id 2]))))))

(deftest administrator-on-items
  (is (true? (acl administrator "SELECT * FROM items;")))
  (is (true? (acl administrator (-> (select [:*])
                                    (from :items)))))
  (is (true? (acl administrator "INSERT INTO items ;")))
  (is (true? (acl administrator (insert-into "items"))))
  (is (true? (acl administrator "DELETE FROM items WHERE id EQ 125;")))
  (is (true? (acl administrator (-> (delete-from "items")
                                    (where [:= :id 125])))))
  (is (true? (acl administrator "UPDATE items WHERE id EQ 123;")))
  (is (true? (acl administrator (-> (helpers/update "items")
                                    (where [:= :id 123]))))))

(deftest administrator-on-users
  (is (true? (acl administrator "SELECT * FROM users WHERE id EQ 1")))
  (is (true? (acl administrator (-> (select :*)
                                    (from [:users :u]
                                          (where [:= :u.id 1]))))))
  (is (true? (acl administrator "SELECT * FROM users;")))
  (is (true? (acl administrator (-> (select :*)
                                    (from [:users :u])))))
  (is (true? (acl administrator "SELECT * FROM users WHERE user-id EQ 2")))
  (is (true? (acl administrator (-> (select :*)
                                    (from [:users :u])
                                    (where [:= :u.id 2])))))
  (is (true? (acl administrator "INSERT INTO users")))
  (is (true? (acl administrator (insert-into "users"))))
  (is (true? (acl administrator "DELETE FROM users WHERE id EQ 1")))
  (is (true? (acl administrator (-> (delete-from :users)
                                    (where [:= :id 1])))))
  (is (true? (acl administrator "DELETE FROM users WHERE users.id EQ 2")))
  (is (true? (acl administrator (-> (delete-from :users)
                                    (where [:= :users.id 2])))))
  (is (true? (acl administrator "DELETE FROM users WHERE id EQ 2")))
  (is (true? (acl administrator (-> (delete-from :users)
                                    (where [:= :id 2])))))
  (is (true? (acl administrator "UPDATE users WHERE user-id EQ 1")))
  (is (true? (acl administrator (-> (helpers/update :users)
                                    (where [:= :id 1])))))
  (is (true? (acl administrator "UPDATE users WHERE user-id EQ 2")))
  (is (true? (acl administrator (-> (helpers/update :users)
                                    (where [:= :id 2]))))))

(deftest administrator-on-addresses
  (is (true? (acl administrator "SELECT * FROM addresses WHERE id EQ 1")))
  (is (true? (acl administrator
                  (-> (select :*)
                      (from :addresses)
                      (where [:= :id 1])))))
  (is (true? (acl administrator "UPDATE addresses WHERE user-id EQ 1;")))
  (is (true? (acl administrator
                  (-> (helpers/update :*)
                      (from :addresses)
                      (where [:= :user-id 1])))))
  (is (= true
         (acl administrator
              "SELECT * FROM addresses;")
         (acl administrator
              (-> (select :*)
                  (from :addresses)))
         (acl administrator
              (-> (select :*)
                  (from "addresses")))
         (acl administrator
              (-> (select :*)
                  (from [:addresses :users])))))
  (is (true? (acl administrator
                  "SELECT * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 1")))
  (is (true? (acl administrator
                  (-> (select :*)
                      (from :addresses)
                      (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                      (merge-join :users [:= :users.id :postal-addresses.user-id])
                      (where [:= :users.id 1])))))
  (is (true? (acl administrator
                  "SELECT * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 2")))
  (is (true? (acl administrator
                  (-> (select :*)
                      (from :addresses)
                      (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                      (merge-join :users [:= :users.id :postal-addresses.user-id])
                      (where [:= :users.id 2])))))
  (is (true? (acl administrator
                  "UPDATE addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 1")))
  (is (true? (acl administrator
                  (-> (helpers/update :addresses)
                      (join :postal-addresses [:= :addresses.id :postal-addresses.address-id])
                      (merge-join :users [:= :users.id :postal-addresses.user-id])
                      (where [:= :users.id 1])))))
  (is (true? (acl administrator
                  "UPDATE * FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 2")))
  (is (true? (acl administrator
                  (-> (helpers/update :addresses)
                      (join [:postal-addresses :p] [:= :addresses.id :p.address-id])
                      (merge-join :users [:= :users.id :p.user-id])
                      (where [:= :users.id 2])))))
  (is (true? (acl administrator
                  "DELETE FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 1")))
  (is (true? (acl administrator
                  (-> (delete-from :addresses)
                      (join [:postal-addresses :p] [:= :addresses.id :p.address-id])
                      (merge-join :users [:= :users.id :p.user-id])
                      (where [:= :users.id 1])))))
  (is (true? (acl administrator
                  "DELETE FROM addresses JOIN postal-addresses ON addresses.id = postal-addresses.addresses-id JOIN users ON users.id = postal-addresses.user-id WHERE users.id EQ 2")))
  (is (true? (acl administrator
                  (-> (delete-from :addresses)
                      (join [:postal-addresses :p] [:= :addresses.id :p.address-id])
                      (merge-join :users [:= :users.id :p.user-id])
                      (where [:= :users.id 2]))))))
