(ns xiana.db.client.postgres
  (:require [xiana.db.protocol :as db-protocol]
            [clj-test-containers.core :as tc]
            [hikari-cp.core :as hcp]
            [honeysql-postgres.format]
            [honeysql.core :as sql]
            [jsonista.core :as json]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]
            [xiana.db.migrate :as migr])
  (:import
   (clojure.lang
    IPersistentMap
    IPersistentVector)
   (java.lang
    AutoCloseable)
   (java.sql
    Connection
    PreparedStatement)
   (org.postgresql.util
    PGobject)))

(def mapper (json/object-mapper {:decode-key-fn keyword}))
(def ->json json/write-value-as-string)
(def <-json #(json/read-value % mapper))
(def default-opts {:return-keys true})

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

(defn get-pool-datasource
  [{:xiana/keys [hikari-pool-params postgresql]}]
  (when (and hikari-pool-params postgresql)
    (fn [{:keys [port dbname host dbtype user password]}]
      (hcp/make-datasource
        (merge {:adapter            dbtype
                :username           user
                :password           password
                :database-name      dbname
                :server-name        host
                :port-number        port}
               hikari-pool-params)))))

(defn get-datasource
  ([config]
   (get-datasource config 0))
  ([config count]
   (let [create-datasource (or (:xiana/create-custom-datasource config)
                               (get-pool-datasource config)
                               jdbc/get-datasource)
         jdbc-opts (merge default-opts
                          (:xiana/jdbc-opts config))]
     (try (-> config
              :xiana/postgresql
              create-datasource
              (jdbc/with-options jdbc-opts))
          (catch Exception e (if (< count 10)
                               (get-datasource config (inc count))
                               (throw e)))))))

(defn docker-postgres!
  [{pg-config :xiana/postgresql :as config}]
  (let [{:keys [dbname user password image-name]} pg-config
        container (tc/start!
                   (tc/create
                    {:image-name    image-name
                     :exposed-ports [5432]
                     :env-vars      {"POSTGRES_DB"       dbname
                                     "POSTGRES_USER"     user
                                     "POSTGRES_PASSWORD" password}}))

        port (get (:mapped-ports container) 5432)
        pg-config (assoc
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
   (try
     (migr/migrate (migr/get-db-config config))
     (catch Exception e (if (< count 10)
                          (migrate! config (inc count))
                          (throw e))))
   config))

(defn connect
  "Adds `:datasource` key to the `:xiana/postgresql` config section
  and duplicates `:xiana/postgresql` under the top-level `:db` key."
  [{pg-config :xiana/postgresql :as config}]
  (let [pg-config (assoc-in pg-config [:config :datasource] (get-datasource config))]
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
   {:pre [(instance? Connection tx)]}
   (let [sql-params (->sql-params sql-map)]
     (jdbc/execute! tx sql-params (merge default-opts jdbc-opts)))))

(defn multi-execute!
  [datasource {:keys [queries transaction?]}]
  (if transaction?
    (jdbc/with-transaction
      [tx datasource]
      (mapv #(in-transaction tx % (:options datasource)) queries))
    (mapv #(execute datasource %) queries)))

(defrecord PostgresDB [config jdbc-opts embedded]
  db-protocol/DatabaseP
  (->db-object [_this obj]
    (->pgobject obj))

  (<-db-object [_this obj]
    (<-pgobject obj))

  (define-container [this]
    (docker-postgres! (:config this)))

  (define-migration [this]
    (migrate! (:config this)))

  (define-migration [this count]
    (migrate! (:config this) count))

  (connect [this]
    (connect (:config this)))

  (define-parameters [_this sql-map]
    (->sql-params sql-map))

  (execute [this sql-map]
    (let [ds (get-in this [:config :datasource])]
      (execute ds sql-map)))

  (in-transaction [_this tx sql-map]
    (in-transaction tx sql-map))

  (multi-execute [this query-map]
    (let [ds (get-in this [:config :datasource])]
      (multi-execute! ds query-map)))

  AutoCloseable
  (close [this]
    (when-let [emb (embedded this)]
      (.close emb))))

(defn create-postgres-DB [config jdbc-opts]
  (->PostgresDB config jdbc-opts nil))

(def db-access
  "Database access interceptor, works from `:query` and from `db-queries` keys
  Enter: nil.
  Leave: Fetch and execute a given query using the chosen database
  driver, if succeeds associate its results into state response data.
  Remember the entry query must be a sql-map, e.g:
  {:select [:*] :from [:users]}."
  {:leave
   (fn [{query-or-fn   :query
         db-queries    :db-queries
         :as        state}]
     (let [datasource (get-in state [:deps :db :config :datasource])
           query (cond
                   (fn? query-or-fn) (query-or-fn state)
                   :else query-or-fn)
           db-data (cond-> []
                     query (into (execute datasource query))
                     db-queries (into (multi-execute! datasource db-queries))
                     :always seq)]
       (assoc-in state [:response-data :db-data] db-data)))
   :error
   (fn [state]
     (merge state
            {:response {:status 500
                        :body   (pr-str (:error state))}}))})


;; For config-map

;; dbtype
;; classname
;; port
;; dbname
;; user
;; datasource
