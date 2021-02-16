(ns framework.db.acl-test
  (:require
    [clojure.test :refer :all]
    [framework.db.acl :refer [->roles
                              insert-action
                              acl]]))

(deftest get-roles-from-query
  (is (= '({:table "test_table" :actions [:select]}) (->roles "SELECT * FROM test_table")))
  (is (= '({:table "films" :actions [:insert]}) (->roles "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');"))) (is (= '([:insert "films"]) (->roles "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '({:table "producers" :actions [:select]} {:table "films" :actions [:delete]}) (->roles "DELETE FROM films\n  WHERE producer_id IN (SELECT id FROM producers WHERE name = 'foo');"))) (is (= '([:insert "films"]) (->roles "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '({:actions [:update] :table "films"}) (->roles "UPDATE films SET kind = 'Dramatic' WHERE kind = 'Drama';"))) (is (= '([:insert "films"]) (->roles "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '({:actions [:drop] :table "conversation"}) (->roles "DROP TABLE conversation;")))
  (is (= '({:actions [:insert] :table "films"}) (->roles "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '({:actions [:truncate] :table "bigtable"}) (->roles "TRUNCATE bigtable;")))
  (is (= '({:actions [:alter] :table "employees"}) (->roles "ALTER TABLE employees ADD COLUMN address text")))
  (is (= '({:actions [:alter] :table "employees"}) (->roles "ALTER TABLE employees DROP COLUMN address"))))

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
                          :actions #{:update :select}}] "films" :select))))

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
                       {:administrator [{:tables  :all
                                         :actions :all
                                         :filter  :all}]}]})

(defn fetch-db
  [db table where]
  (if-let [found (filter where (get db table))]
    (if (next found) found (first found))))

(is (= {:administrator [{:tables  :all
                         :actions :all
                         :filter  :all}]}
       (fetch-db mock-db :roles :administrator)))

(defn add-user-by-id
  [env user-id]
  (let [user (fetch-db mock-db :users #(= user-id (:id %)))
        role (:role user)]
    (cond-> env
      user (assoc-in [:session :user] user)
      role (assoc-in [:session :user :roles] (get (fetch-db mock-db :roles role) role)))))

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
         (add-user-by-id {} 1))))

(is (acl (add-user-by-id {} 1) "SELECT * FROM items;"))
(is (not (acl (add-user-by-id {} 1) "DELETE * FROM items;")))
(is (acl (add-user-by-id {} 2) "DELETE FROM carts WHERE user-id EQ 2"))
(is (acl (add-user-by-id {} 1) "DELETE FROM items;"))
(is (acl (add-user-by-id {} 1) "INSERT * FROM items ;"))
(is (acl (add-user-by-id {} 1) "UPDATE * FROM items ;"))
(is (acl (add-user-by-id {} 1) "SELECT * FROM items ;"))
(is (acl (add-user-by-id {} 1) "SELECT * FROM items ;"))
(is (acl (add-user-by-id {} 1) "SELECT * FROM items ;"))
(is (acl (add-user-by-id {} 1) "SELECT * FROM items ;"))
(is (acl (add-user-by-id {} 1) "SELECT * FROM items ;"))
