(ns framework.db.core
  "Data source builder"
  (:require
    [next.jdbc :as jdbc]
    [honeysql-postgres.format]
    [honeysql.core :as sql]
    [xiana.core :as xiana])
  (:import
    (javax.sql
      DataSource)
    (org.postgresql.jdbc
      PgConnection)))


(defn start
  "Creates datasource.
  When no parameter given, then resolves database specs from configuration"
  [{:framework.db.storage/keys [postgresql]
    :as                        config}]
  (assoc config
         :datasource
         (jdbc/get-datasource postgresql)))

(defn ->sql-params
  "Parse sql-map using honeysql format function with pre-defined
  options that target postgresql."
  [sql-map]
  (sql/format sql-map
              {:quoting            :ansi
               :parameterizer      :postgresql
               :return-param-names false}))

(defn execute!
  "Gets datasource, parse the given sql-map (query) and
  execute it using `jdbc/execute!`, and returns the modified keys"
  [datasource sql-map]
  {:pre [(instance? DataSource datasource)]}
  (with-open [connection (.getConnection datasource)]
    (let [sql-params (->sql-params sql-map)]
      (jdbc/execute! connection sql-params {:return-keys true}))))

(defn in-transaction
  [tx sql-map]
  {:pre [(instance? PgConnection tx)]}
  (let [sql-params (->sql-params sql-map)]
    (jdbc/execute! tx sql-params {:return-keys true})))

(defn multi-execute!
  [datasource {:keys [queries transaction?]}]
  (if transaction?
    (jdbc/with-transaction [tx datasource]
                           (mapv #(in-transaction tx %) queries))
    (mapv #(execute! datasource %) queries)))

(def db-access
  "Database access interceptor, works from `:query` and from `db-queries` keys
  Enter: nil.
  Leave: Fetch and execute a given query using the chosen database
  driver, if succeeds associate its results into state response data.
  Remember the entry query must be a sql-map, e.g:
  {:select [:*] :from [:users]}."
  {:leave
   (fn [{query      :query
         db-queries :db-queries
         :as        state}]
     (let [datasource (get-in state [:deps :db :datasource])
           db-data (cond-> []
                           query (into (execute! datasource query))
                           db-queries (into (multi-execute! datasource db-queries))
                           :always seq)]
       (xiana/ok
         (assoc-in state [:response-data :db-data] db-data))))})