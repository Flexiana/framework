(ns xiana.db
  "Data source builder"
  (:require
    [clj-test-containers.core :as tc]
    [honeysql-postgres.format]
    [honeysql.core :as sql]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.core :as xiana])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)
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
   (try (jdbc/get-datasource (:xiana/postgresql config))
        (catch Exception e (if (< count 10)
                             (get-datasource config (inc count))
                             (throw e))))))

(defn embedded-postgres!
  [config init-sql]
  (let [pg (-> (EmbeddedPostgres/builder)
               (.start))
        ds (.getPostgresDatabase pg)
        pg-port (.getPort pg)
        db-config (-> config
                      :xiana/postgresql
                      (assoc
                        :port pg-port
                        :dbname "postgres"
                        :user "postgres"
                        :embedded pg
                        :datasource ds))]
    (when (seq init-sql) (jdbc/execute! (dissoc db-config :dbname) init-sql))
    (assoc config :xiana/postgresql db-config)))

(defn docker-postgres!
  [config init-sql]
  (let [{db-name  :dbname
         user     :user
         password :password} (:xiana/postgresql config)
        container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :exposed-ports [5432]
                                  :env-vars      {"POSTGRES_DB"       db-name
                                                  "POSTGRES_USER"     user
                                                  "POSTGRES_PASSWORD" password}})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)
        db-config (-> config
                      :xiana/postgresql
                      (assoc
                        :port port
                        :embedded container
                        :subname (str "//localhost:" port "/" db-name)))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (when (seq init-sql) (jdbc/execute! (dissoc db-config :dbname) init-sql))
    (assoc config :xiana/postgresql db-config)))

(defn migrate!
  ([config]
   (migrate! config 0))
  ([config count]
   (try (let [db (:xiana/postgresql config)
              mig-config (assoc (:xiana/migration config) :db db)]
          (migratus/migrate mig-config))
        (catch Exception e (if (< count 10)
                             (migrate! config (inc count))
                             (throw e))))
   config))

(defn start
  "Creates datasource.
  When no parameter given, then resolves database specs from configuration"
  [config]
  (let [db-spec (:xiana/postgresql config config)
        init-sql (when-let [sql (some-> (:init-script db-spec) slurp)] [sql])
        db-instance (case (:deployment db-spec)
                      :container (docker-postgres! config init-sql)
                      :embedded (embedded-postgres! config init-sql)
                      config)
        datasource (get-in db-instance [:xiana/postgresql :datasource]
                           (get-datasource db-instance))
        db-config (assoc db-instance :datasource datasource)]
    (assoc config
           :xiana/postgresql db-config
           :db db-config)))

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
