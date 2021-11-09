(ns framework.db.test-support
  (:require
    [clj-test-containers.core :as tc]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)))

(defn embedded-postgres!
  [config init-sql]
  (let [pg (-> (EmbeddedPostgres/builder)
               (.start))
        ds (.getPostgresDatabase pg)
        pg-port (.getPort pg)
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port
                        :dbname "postgres"
                        :user "postgres"
                        :embedded pg
                        :datasource ds))]
    (when (seq init-sql) (jdbc/execute! (dissoc db-config :dbname) init-sql))
    (assoc config :framework.db.storage/postgresql db-config)))

(defn docker-postgres!
  [config init-sql]
  (let [{db-name  :dbname
         user     :user
         password :password} (:framework.db.storage/postgresql config)
        container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :exposed-ports [5432]
                                  :env-vars      {"POSTGRES_DB"       db-name
                                                  "POSTGRES_USER"     user
                                                  "POSTGRES_PASSWORD" password}})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port port
                        :embedded container
                        :subname (str "//localhost:" port "/" db-name)))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (when (seq init-sql) (jdbc/execute! (dissoc db-config :dbname) init-sql))
    (assoc config :framework.db.storage/postgresql db-config)))
