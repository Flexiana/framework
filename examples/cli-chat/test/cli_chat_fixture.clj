(ns cli-chat-fixture
  (:require
    [cli-chat.core :refer [system]]
    [framework.config.core :as config]
    [framework.webserver.core :as ws]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)))


(defn embedded-postgres!
  [config]
  (let [pg (.start (EmbeddedPostgres/builder))
        pg-port (.getPort pg)
        init-sql (slurp "./Docker/init.sql")
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port
                        :embedded pg
                        :subname (str "//localhost:" pg-port "/cli-chat")))]
    (jdbc/execute! (dissoc db-config :dbname) [init-sql])
    (assoc config :framework.db.storage/postgresql db-config)))

(defn migrate!
  [config]
  (let [db (:framework.db.storage/postgresql config)
        mig-config (assoc (:framework.db.storage/migration config) :db db)]
    (migratus/migrate mig-config))
  config)

(defn std-system-fixture
  [config f]
  (try
    (-> (config/env)
        (merge config)
        embedded-postgres!
        migrate!
        system)
    (f)
    (finally
      (ws/stop))))
