(ns framework.db.acl-test
  (:require [clojure.test :refer :all])
  (:require [framework.db.acl :refer [action]]))



(deftest action-test
  (is (= '([:select "test_table"]) (action "SELECT * FROM test_table")))
  (is (= '([:insert "films"]) (action "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');"))) (is (= '([:insert "films"]) (action "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '([:delete "films"] [:select "producers"]) (action "DELETE FROM films\n  WHERE producer_id IN (SELECT id FROM producers WHERE name = 'foo');"))) (is (= '([:insert "films"]) (action "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '([:update "films"]) (action "UPDATE films SET kind = 'Dramatic' WHERE kind = 'Drama';"))) (is (= '([:insert "films"]) (action "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '([:drop "conversation"]) (action "DROP TABLE conversation;")))
  (is (= '([:insert "films"]) (action "INSERT INTO films (code, title, did, date_prod, kind)\n    VALUES ('T_601', 'Yojimbo', 106, '1961-06-16', 'Drama');")))
  (is (= '([:truncate "bigtable"]) (action "TRUNCATE bigtable;")))
  (is (= '([:alter "employees"]) (action "ALTER TABLE employees ADD COLUMN address text")))
  (is (= '([:alter "employees"]) (action "ALTER TABLE employees DROP COLUMN address"))))

(def mock-db
  {:users             [{:id              1
                        :name            "John"
                        :surname         "Doe"
                        :email           "doe.john@test.com"
                        :role            :customer}
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
                       {:id              6
                        :name            "Janet"
                        :surname         "Doe"
                        :email           "doe.janet@test.com"
                        :role            :customer}]
   :items [{:id 1
            :name "salami slicer"
            :price {:value 28.49
                    :currency "USD"}}
           {:id 2
            :name "bread knife"
            :price {:value 12.97
                    :currency "USD"}}
           {:id 3
            :name "Meat tenderizer"
            :price {:value 14.99
                    :currency "USD"}}
           {:id 4
            :name "cheese grater"
            :price {:value 12.49
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
   :roles             [{:customer [{:select "items"
                                    :filter :all}
                                   {:select "users"
                                    :filter :own}
                                   {:update "users"
                                    :filter :own}
                                   {:delete "users"
                                    :filter :own}
                                   {:insert "addresses"
                                    :filter :own}
                                   {:update "addresses"
                                    :filter :own}
                                   {:select "addresses"
                                    :filter :own}
                                   {:delete "addresses"
                                    :filter :own}
                                   {:select "carts"
                                    :filter :own}
                                   {:insert "carts"
                                    :filter :own}
                                   {:update "carts"
                                    :filter :own}
                                   {:delete "carts"
                                    :filter :own}]}
                       {:warehouse-worker [{:select "items"
                                            :filter :all}
                                           {:update "carts"
                                            :filter :all}
                                           {:select "carts"
                                            :filter :all}]}
                       {:postal-worker [{:select "carts"
                                         :filter :all}
                                        {:update "carts"
                                         :filter :all}
                                        {:select :users
                                         :filter :all}
                                        {:select "addresses"
                                         :filter :all}]}
                       {:shop-worker [{:all    "items"
                                       :filter :all}]}
                       {:administrator [{:all    :all
                                         :filter :all}]}]})
