(ns xiana.db-provisional
  (:require [xiana.db.client.postgres :as pg]
            [xiana.db.client.mysql :as mysql]
            [taoensso.timbre :as log]))

(defn generate-table-existence-query
  [table-name dbms db-name]
  (case dbms
    "mysql" {:select [:table_name :table_schema]
             :from [:information_schema.tables]
             :where [:and
                     [:= :table_schema db-name]
                     [:= :table_name table-name]]}
    "postgresql" {:select [:table_name]
                  :from [:information_schema.tables]
                  :where [:and
                          [:= :table_schema "public"]
                          [:= :table_name table-name]]
                  :limit 1}))

(defn table-exists?
  [table-name connection-object]
  (let [db-cfg (:config connection-object)
        dbms (:dbtype db-cfg)
        db-name (:dbname db-cfg)
        query (generate-table-existence-query (name table-name) dbms db-name)
        _ (log/info "== Verifying table existence ==")
        result (.execute connection-object query)
        _ (log/info result)]
    (not (empty? result))))

(defn construct
  "Useful to create instances of dynamic classes"
  [dbms config opts embedded]
  (case dbms
   :xiana/mysql (mysql/->MySQLDB config opts embedded)
   :xiana/postgresql (pg/->PostgresDB config opts embedded)))



{:select [:table_name :table_schema] :from [:information_schema.tables] :where [:and [:= :table_schema "public"] [:= :table_name "bla"]]}