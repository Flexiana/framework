(ns framework.db.core-test
  (:require
   [next.jdbc :as jdbc]
   [clojure.test :refer :all]
   [framework.config.core :as config]
   [framework.db.core :as db]))

(deftest contains-datasource
  ;; start the connection
  (db/start)
  ;; verify if the datasource was persisted
  (is (not (nil? (:datasource @db/db)))))

(deftest contains-connection
  (let [connection (db/connection)]
    ;; verify if the connection was established
    (is (not (nil? connection)))))

(deftest restart-connection
  ;; clean db connection
  (swap! db/db {:datasource (jdbc/get-datasource (config/get-spec :database))
                :connection nil})
  ;; connect again and verify it
  (let [connection (db/connection)]
    (is (not (nil? connection)))))

(deftest contains-nil-datasource
  ;; reset db instance
  (reset! db/db {})
  ;; start with an empty db-spec
  (db/start {})
  ;; verify if the connection is nil
  (is (nil? (:datasource @db/db))))

(deftest contains-nil-connection
  ;; reset db instance
  (reset! db/db {})
  ;; set a wrong db-spec
  (let [db-spec {:port     0,
                 :dbname   "framework",
                 :host     "localhost",
                 :dbtype   "postgresql",
                 :user     "postgres",
                 :password "postgres"}]
    ;; start the database instance
    (db/start db-spec)
    ;; verify if the connection is nil
    (is (nil? (:connection @db/db)))))
