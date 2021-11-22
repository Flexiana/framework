(ns framework.db.core
  "Data source builder"
  (:require
    [clj-test-containers.core :as tc]
    [honeysql-postgres.format]
    [honeysql.core :as sql]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.core :as xiana])
  (:import
    (java.lang
      AutoCloseable)
    (javax.sql
      DataSource)
    (org.postgresql.jdbc
      PgConnection)))

(defrecord db
  [dbtype
   classname
   port
   dbname
   user
   embedded
   datasource]
  AutoCloseable
  (close [this]
    (when-let [emb (embedded this)]
      (.close emb))))

(defn- get-datasource
  ([config]
   (get-datasource config 0))
  ([config count]
   (try (jdbc/get-datasource (:framework.db.storage/postgresql config))
        (catch Exception e (if (< count 10)
                             (get-datasource config (inc count))
                             (throw e))))))

(defn docker-postgres!
  [{pg-config :framework.db.storage/postgresql :as config}]
  (let [{:keys [dbname user password image-name]} pg-config
        container            (-> (tc/create
                                   {:image-name    image-name
                                    :exposed-ports [5432]
                                    :env-vars      {"POSTGRES_DB"       dbname
                                                    "POSTGRES_USER"     user
                                                    "POSTGRES_PASSWORD" password}})
                                 (tc/start!))
        port                 (get (:mapped-ports container) 5432)
        pg-config            (assoc
                               pg-config
                               :port port
                               :embedded container
                               :subname (str "//localhost:" port "/" dbname))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (assoc config :framework.db.storage/postgresql pg-config)))

(defn migrate!
  ([config]
   (migrate! config 0))
  ([config count]
   (try (let [db (:framework.db.storage/postgresql config)
              mig-config (assoc (:framework.db.storage/migration config) :db db)]
          (migratus/migrate mig-config))
        (catch Exception e (if (< count 10)
                             (migrate! config (inc count))
                             (throw e))))
   config))

(defn connect
  "Adds `:datasource` key to the `:framework.db.storage/postgresql` config section
  and duplicates `:framework.db.storage/postgresql` under the top-level `:db` key."
  [{pg-config :framework.db.storage/postgresql :as config}]
  (let [pg-config (assoc pg-config :datasource (get-datasource config))]
    (assoc config
           :framework.db.storage/postgresql pg-config
           :db pg-config)))

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
    (jdbc/with-transaction
      [tx datasource]
      (mapv #(in-transaction tx %) queries))
    (mapv #(execute datasource %) queries)))

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
                     query (into (execute datasource query))
                     db-queries (into (multi-execute! datasource db-queries))
                     :always seq)]
       (xiana/ok
         (assoc-in state [:response-data :db-data] db-data))))})
