(ns framework.db.sql
  "PostgreSQL functions, helpers, and executor"
  (:require
    [honeysql-postgres.format]
    [honeysql-postgres.helpers :as helpers]
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

(defn ->sql-params
  "Parse sql-map using honeysql format function with pre-defined
  options that target postgresql."
  [sql-map]
  (sql/format sql-map
              {:quoting            :ansi
               :parameterizer      :postgresql
               :return-param-names false}))

(defn execute
  "Gets datasource, parse the given sql-map (query) and
  execute it using `jdbc/execute!`, and returns the modified keys"
  [datasource sql-map]
  (with-open [connection (.getConnection datasource)]
    (let [sql-params (->sql-params sql-map)]
      (jdbc/execute! connection sql-params {:return-keys true}))))
