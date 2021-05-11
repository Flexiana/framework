(ns framework.db.sql
  (:require
   [next.jdbc :as jdbc]
   [honeysql.core :as sql]
   [framework.db.core :as db]
   [honeysql-postgres.format]
   [clojure.string :as string]
   [potemkin :refer [import-vars]]
   [honeysql-postgres.helpers :as helpers]))

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

(defmulti build-clause
  "Create build clause multimethod with associated
  dispatch function: (honeysql-postgres.helpers args)."
  (fn [optype dbtype _args] [dbtype optype]))

(defmethod build-clause [:default :create-table]
  [_ _ args] (helpers/create-table (:table-name args)))

(defmethod build-clause [:default :drop-table]
  [_ _ args] (helpers/drop-table (:table-name args)))

(defmethod build-clause [:default :with-columns]
  [_ _ args]
  (let [{:keys [map rows]} args]
    (helpers/with-columns map rows)))

;; TODO: research!
(defn fmt-create-table-stmt
  "This function should not exist.
  Research why jdbc cant process create statements with ?."
  [sql-map]
  (let [sql-vec (sql/format sql-map)
        query (reduce (fn [s input]
                        (string/replace-first s #"\?" (str input)))
                      sql-vec)]
    [query]))

(defn execute
  "Get connection and passes the sql-params to `jdbc/execute!`."
  [sql-params]
  (with-open [connection (db/connection)]
    (jdbc/execute! connection
                   sql-params
                   {:return-keys true})))

(defn execute!
  "Get connection and parse the sql-params to `jdbc/execute!`."
  [sql-params]
  (with-open [connection (db/connection)]
    (jdbc/execute! connection sql-params)))

(defn create-table
  "Create table specified by its name on the database."
  ([table-name]
   (create-table table-name {:dbtype :default}))
  ([table-name opts]
   (let [dbtype (:dbtype opts)
         args   {:table-name table-name}]
     (build-clause :create-table dbtype args))))

(defn drop-table
  "Delete table."
  ([table-name]
   (drop-table table-name {:dbtype :default}))
  ([table-name opts]
   (let [dbtype (:dbtype opts)
         args   {:table-name table-name}]
     (build-clause :drop-table dbtype args))))

(defn with-columns
  "Dispatch database operation with columns arguments."
  [m rows]
  (let [args {:map m :rows rows}]
    (build-clause :with-columns :default args)))
