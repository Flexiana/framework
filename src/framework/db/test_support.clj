(ns framework.db.test-support
  (:require
    [clj-test-containers.core :as tc]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)
    (java.lang
      AutoCloseable)))

(defn embedded-postgres!
  [config init-sql]
  (let [pg (.start (EmbeddedPostgres/builder))
        pg-port (.getPort pg)
        db-name (-> config
                    :framework.db.storage/postgresql :dbname)
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port
                        :embedded pg
                        :subname (str "//localhost:" pg-port db-name)))]
    (jdbc/execute! (dissoc db-config :dbname) init-sql)
    (assoc config :framework.db.storage/postgresql db-config)))

(defn docker-postgres!
  [{:framework.db.storage/keys [postgresql]
    :as                        config}
   init-sql]
  (let [{:as   postgres-cfg} postgresql
        {:keys [mapped-ports container]
         :as   container-component} (->> {:image-name    "postgres:11.5-alpine"
                                          :env-vars      {"POSTGRES_DB" "postgres"}
                                          :exposed-ports [5432]}
                                         tc/create
                                         tc/start!)]
    (tc/wait {:wait-strategy :log
              :message "Accepting Connections"} container)
    (let [db-config (assoc postgres-cfg
                           :dbname "postgres"
                           :user "postgres"
                           :password "postgres"
                           :port (get mapped-ports 5432)
                           :embedded (reify AutoCloseable
                                       (close [this]
                                         (tc/stop! container-component)))
                           :subname (str "//localhost:"
                                         (get mapped-ports 5432)
                                         "/postgres"))]
      (jdbc/execute! db-config init-sql)
      (assoc config :framework.db.storage/postgresql db-config))))

(defn migrate!
  [{{:keys [port
            subname
            user
            password]} :framework.db.storage/postgresql
    :as                config}]

  (let [{:framework.db.storage/keys [migration]
         :as new-config} (-> config
                             (assoc-in [:framework.db.storage/migration :db]
                                       {:classname   "org.postgresql.Driver"
                                        :subprotocol "postgres"
                                        :subname     subname
                                        :user        "postgres"
                                        :password    "postgres"}))]
    (migratus/migrate migration)
    new-config))
