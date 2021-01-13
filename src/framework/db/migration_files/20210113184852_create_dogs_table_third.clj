(ns framework.db.migration-files.20210113184852-create-dogs-table-third
  (:require [framework.db.sql :as sql]))

(defn up [config]
  (-> (sql/create-table :dogs)
      (sql/with-columns [[:name (sql/call :varchar 200) (sql/call :primary-key)]
                         [:born_at :date]])
      (sql/execute! config)))

(defn down [config]
  (-> (sql/drop-table :dogs)
      (sql/execute! config)))
