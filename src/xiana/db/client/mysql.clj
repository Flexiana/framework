(ns xiana.db.client.mysql
  (:require [clj-test-containers.core :as tc]
            [next.jdbc :as jdbc] 
            [hikari-cp.core :as hcp]
            [honeysql.core :as sql]
            [jsonista.core :as jsonista]
            [xiana.db.migrate :as migr]
            [xiana.db.protocol :as db-protocol]
            [taoensso.timbre :as log])
  (:import [clojure.lang 
            IPersistentMap
            IPersistentVector]
           (java.lang
            AutoCloseable)
           (java.sql
            Connection
            PreparedStatement)
           (java.sql
            Connection)))

(def default-opts {:return-keys true})

(def mapper (jsonista/object-mapper {:decode-key-fn keyword}))

(defn ->json [data]
  (jsonista/write-value-as-string data mapper))

(defn <-json [json-str]
  (jsonista/read-value json-str mapper))

;; (defn ->mysqlobject [x]
;;   (let [mysqltype "jsonb"]
;;     (doto (com.mysql.cj.api.json.JsonString. (->json x))
;;       (.setType mysqltype))))

;; (defn <-mysqlobject [v]
;;   (let [type (.getType v)
;;         value (.getValue v)]
;;     (if (#{"jsonb" "json"} type)
;;       (some-> value
;;               <-json)
;;       value)))

;; (extend-protocol prepare/SettableParameter
;;   IPersistentMap
;;   (set-parameter [^IPersistentMap m ^PreparedStatement s i]
;;     (.setObject s i (->mysqlobject m)))

;;   IPersistentVector
;;   (set-parameter [^IPersistentVector v ^PreparedStatement s i]
;;     (.setObject s i (->mysqlobject v))))

;; (extend-protocol rs/ReadableColumn 
;;   (read-column-by-label [v _]
;;     (<-mysqlobject v))
;;   (read-column-by-index [v _ _]
;;     (<-mysqlobject v)))

(defn get-pool-datasource
  [{:xiana/keys [hikari-pool-params mysql]}]
  (when (and hikari-pool-params mysql)
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
              create-datasource
              (jdbc/with-options jdbc-opts))
          (catch Exception e (if (< count 10)
                               (get-datasource config (inc count))
                               (throw e)))))))

(defn docker-mysql!
  [{mysql-config :xiana/mysql :as config}]
  (let [{:keys [dbname user password image-name]} mysql-config
        container (tc/start!
                   (tc/create
                    {:image-name    image-name
                     :exposed-ports [5432]
                     :env-vars      {"MYSQL_DB"       dbname
                                     "MYSQL_USER"     user
                                     "MYSQL_PASSWORD" password}}))

        port (get (:mapped-ports container) 5432)
        mysql-config (assoc
                      mysql-config
                      :port port
                      :embedded container
                      :subname (str "//localhost:" port "/" dbname))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (assoc config :xiana/mysql mysql-config)))

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
  "Adds `:datasource` key to the `:xiana/mysql` config section
  and duplicates `:xiana/mysql` under the top-level `:db` key."
  [{mysql-config :xiana/mysql :as config}]
  (let [new-mysql-config (assoc-in mysql-config [:config :datasource] (get-datasource (:config mysql-config)))]
    (assoc config
           :xiana/mysql (:config new-mysql-config)
           :db (:config new-mysql-config))))

(defn ->sql-params
  "Parse sql-map using honeysql format function with pre-defined
  options that target mysql."
  [sql-map] 
  (if (not= (type sql-map) clojure.lang.PersistentVector)
    (sql/format sql-map
                {;:quoting            :ansi
                 :dialect            :mysql
                 :parameterizer      :mysql
                 :return-param-names false})
    (do
      (log/info "Trying to return a raw sql")
      sql-map)))

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

(defrecord MySQLDB [config jdbc-opts embedded]
  db-protocol/DatabaseP

  (define-container [_this]
    (docker-mysql! config))

  (define-migration [_this]
    (migrate! config))

  (define-migration [_this count]
    (migrate! config count))

  (connect [_this]
    (let [new-config {:xiana/mysql config
                      :xiana/jdbc-opts jdbc-opts}]
      (connect new-config)))

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
     (let [datasource (get-in state [:deps :db :datasource])
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



