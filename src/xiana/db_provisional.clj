(ns xiana.db-provisional
  (:require [xiana.db.client.postgres :as pg]
            [xiana.db.client.mysql :as mysql]))

(def dbms-map {:xiana/postgresql pg/->PostgresDB
               :xiana/mysql mysql/->MySQLDB})

