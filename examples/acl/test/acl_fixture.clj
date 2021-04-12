(ns acl-fixture
  (:require
    [acl]
    [com.stuartsierra.component :as component]
    [framework.config.core :as config]
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
                        :subname (str "//localhost:" pg-port "/acl")))]
    (jdbc/execute! (dissoc db-config :dbname) [init-sql])
    (assoc config :framework.db.storage/postgresql db-config)))

(defn migrate!
  [config]
  (let [db (:framework.db.storage/postgresql config)
        mig-config (assoc (:framework.db.storage/migration config) :db db)]
    (migratus/migrate mig-config))
  config)

(defn std-system-fixture
  [f]
  (let [config (config/edn)
        system (-> config
                   embedded-postgres!
                   migrate!
                   acl/system
                   component/start)]
    (try
      (f)
      (finally
        (.close (get-in system [:db :config :embedded]))
        (component/stop system)))))

