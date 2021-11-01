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
  [config init-sql]
  (let [container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :exposed-ports [5432]})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)
        db-name (-> config
                    :framework.db.storage/postgresql :dbname)
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port port
                        :embedded container
                        :user "postgres"
                        :subname (str "//localhost:" port db-name)))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (jdbc/execute! (dissoc db-config :dbname) init-sql)
    (assoc config :framework.db.storage/postgresql db-config)))

(defn migrate!
  [config]
  (let [db (:framework.db.storage/postgresql config)
        mig-config (assoc (:framework.db.storage/migration config) :db db)]
    (migratus/migrate mig-config))
  config)
