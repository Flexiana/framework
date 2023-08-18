(ns xiana.db-provisional
  (:require [xiana.db.client.postgres :as pg]))

(def dbms-map {:xiana/postgresql pg/->PostgresDB})

