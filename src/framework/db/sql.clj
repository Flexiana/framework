(ns framework.db.sql
  (:require
    [clojure.string :as string]
    [honeysql-postgres.format]
    [honeysql-postgres.helpers :as psqlh]
    [honeysql.core :as sql]
    [next.jdbc :as jdbc]
    [potemkin :refer [import-vars]]))

(import-vars
  [honeysql.helpers
   select
   merge-select
   un-select
   from
   merge-from
   join
   merge-join
   left-join
   merge-left-join
   merge-right-join
   full-join
   merge-full-join
   cross-join
   merge-group-by
   order-by
   merge-order-by
   limit
   offset
   lock
   modifiers
   where])

(import-vars
  [honeysql.core
   call])

(defn fmt-create-table-stmt
  "This function should not exist. Research why jdbc cant process
  create statements with ?."
  [hsql]
  (let [qry-raw (sql/format hsql)
        qry (reduce
              (fn [s input]
                (string/replace-first s #"\?" (str input)))
              (first qry-raw)
              (rest qry-raw))]
    [qry]))

(defn execute
  "Executes db query"
  [state query]
  (jdbc/execute!
    (get-in state [:deps :db :datasource]) query {:return-keys true}))

(defn execute!
  [hsql config]
  (let [query (if (:create-table hsql)
                (fmt-create-table-stmt hsql)
                (sql/format hsql))
        conn (if (get-in config [:conn :connection])
               (get-in config [:conn :connection])
               (:connection config))]
    (jdbc/execute! conn query)))

(defmulti build-clause (fn [optype dbtype _args] [dbtype optype]))

(defn create-table
  ([table-name]
   (create-table table-name {:dbtype :default}))
  ([table-name opts]
   (let [{:keys [dbtype]} opts
         args {:table-name table-name}]
     (build-clause :create-table dbtype args))))

(defmethod build-clause [:default :create-table]
  [_ _ args]
  (psqlh/create-table (:table-name args)))

(defn drop-table
  ([table-name]
   (drop-table table-name {:dbtype :default}))
  ([table-name opts]
   (let [{:keys [dbtype]} opts
         args {:table-name table-name}]
     (build-clause :drop-table dbtype args))))

(defmethod build-clause [:default :drop-table]
  [_ _ args]
  (psqlh/drop-table (:table-name args)))

(defn with-columns
  [m rows]
  (let [args {:map m :rows rows}]
    (build-clause :with-columns :default args)))

(defmethod build-clause [:default :with-columns]
  [_ _ args]
  (let [{:keys [map rows]} args]
    (psqlh/with-columns map rows)))
