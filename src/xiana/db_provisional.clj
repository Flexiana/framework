(ns xiana.db-provisional
  (:require [xiana.db.client.postgres :as pg]
            [xiana.db.client.mysql :as mysql]))

(defn construct
  "Useful to create instances of dynamic classes"
  [dbms config opts embedded]
  (case dbms
   :xiana/mysql (mysql/->MySQLDB config opts embedded)
   :xiana/postgresql (pg/->PostgresDB config opts embedded)))