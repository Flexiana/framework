(ns framework.db.sql-test
  (:require
   [clojure.test :refer :all]
   [honeysql.core :as sql]
   [framework.db.sql :as db.sql]
   [honeysql-postgres.helpers :as helpers]))

;; NOTE: using db/execute function
(deftest contains-username
  (let [sql-map {:select [:*]
                 :from   [:users]
                 :where  [:= :users/username "admin"]}
        username (:users/username (first (db.sql/execute sql-map)))
        expected "admin"]
    ;; verify if the retrieved username is the expected
    (is (= username expected))))

(deftest handles-wrong-sql-map-entry
  ;; bind sql-map to string
  (let [sql-map ""
        result (db.sql/execute sql-map)
        expected {}]
    ;; verify if the retrieved result is the expected
    (is (= result expected))))

;; NOTE: using db/execute! function
(deftest contains-user-role
  (let [sql-vec ["SELECT role FROM users WHERE username = ?" "frankie"]
        role (-> (db.sql/execute! sql-vec)
                 (first)
                 (:users/role))
        expected "user"]
    ;; verify if the retrieved role is the expected
    (is (= role expected))))

;; NOTE: using db/execute! function
(deftest contains-empty-response
  (let [sql-vec nil
        result (db.sql/execute! sql-vec)
        expected {}]
    ;; verify if the retrieved result is the expected
    (is (= result expected))))

(deftest contains-create-table-clause
  (let [result (db.sql/create-table "TEST")
        expected {:create-table '("TEST")}]
    ;; verify if the retrieved result is the expected
    (is (and (map? result)
             (= result expected)))))

(deftest contains-drop-table-clause
  (let [result (db.sql/drop-table "TEST")
        expected {:drop-table '("TEST")}]
    ;; verify if the retrieved result is the expected
    (is (and (map? result)
             (= result expected)))))

(deftest contains-with-columns-clause
  (let [result (-> (db.sql/create-table :TEST)
                   (db.sql/with-columns [[:user_id :SERIAL :PRIMARY :KEY]])
                   (db.sql/->sql-params))
        expected ["CREATE TABLE TEST (user_id SERIAL PRIMARY KEY)"]]
    ;; verify if the retrieved result is the expected
    (is (and (vector? result)
             (= result expected)))))
