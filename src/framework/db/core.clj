(ns framework.db.core
  "Data source builder"
  (:require
    [clj-test-containers.core :as tc]
    [honeysql-postgres.format]
    [honeysql.core :as sql]
    [jsonista.core :as json]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [next.jdbc.prepare :as prepare]
    [next.jdbc.result-set :as rs]
    [xiana.core :as xiana])
  (:import
    (clojure.lang
      IPersistentMap
      IPersistentVector)
    (java.lang
      AutoCloseable)
    (java.sql
      PreparedStatement)
    (org.postgresql.jdbc
      PgConnection)
    (org.postgresql.util
      PGobject)))

(def mapper (json/object-mapper {:decode-key-fn keyword}))
(def ->json json/write-value-as-string)
(def <-json #(json/read-value % mapper))

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^PGobject v]
  (let [type (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (some-> value
              <-json
              (with-meta {:pgtype type}))
      value)))

;; Hashmaps and vectors in queries will be converted to PGobject for JSON/JSONB
(extend-protocol prepare/SettableParameter
  IPersistentMap
  (set-parameter [^IPersistentMap m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  IPersistentVector
  (set-parameter [^IPersistentVector v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; PGobject containing JSON/JSONB will be converted to Clojure data
(extend-protocol rs/ReadableColumn
  PGobject
  (read-column-by-label [^PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^PGobject v _ _]
    (<-pgobject v)))

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

(def default-opts {:return-keys true})

(defn get-datasource
  ([config]
   (get-datasource config 0))
  ([config count]
   (let [jdbc-opts (merge default-opts
                          (:xiana/jdbc-opts config))]
     (try (-> config
              :xiana/postgresql
              jdbc/get-datasource
              (jdbc/with-options jdbc-opts))
          (catch Exception e (if (< count 10)
                               (get-datasource config (inc count))
                               (throw e)))))))

(defn docker-postgres!
  [{pg-config :xiana/postgresql :as config}]
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
    (assoc config :xiana/postgresql pg-config)))

(defn migrate!
  ([config]
   (migrate! config 0))
  ([config count]
   (try (let [db-conf    {:datasource (-> config
                                          :xiana/postgresql
                                          :datasource
                                          jdbc/get-datasource)}
              mig-config (assoc (:xiana/migration config)
                                :db db-conf)]
          (migratus/migrate mig-config))
        (catch Exception e (if (< count 10)
                             (migrate! config (inc count))
                             (throw e))))
   config))

(defn connect
  "Adds `:datasource` key to the `:xiana/postgresql` config section
  and duplicates `:xiana/postgresql` under the top-level `:db` key."
  [{pg-config :xiana/postgresql :as config}]
  (let [pg-config (assoc pg-config :datasource (get-datasource config))]
    (assoc config
           :xiana/postgresql pg-config
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
  (let [sql-params (->sql-params sql-map)]
    (jdbc/execute! datasource sql-params default-opts)))

(defn in-transaction
  ([tx sql-map]
   (in-transaction tx sql-map nil))
  ([tx sql-map jdbc-opts]
   {:pre [(instance? PgConnection tx)]}
   (let [sql-params (->sql-params sql-map)]
     (jdbc/execute! tx sql-params (merge default-opts jdbc-opts)))))

(defn multi-execute!
  [datasource {:keys [queries transaction?]}]
  (if transaction?
    (jdbc/with-transaction
      [tx datasource]
      (mapv #(in-transaction tx % (:options datasource)) queries))
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
           db-data    (cond-> []
                        query      (into (execute datasource query))
                        db-queries (into (multi-execute! datasource db-queries))
                        :always    seq)]
       (xiana/ok
         (assoc-in state [:response-data :db-data] db-data))))})
