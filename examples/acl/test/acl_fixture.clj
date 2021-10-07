(ns acl-fixture
  (:require
    [acl]
    [framework.config.core :as config]
    [framework.webserver.core :as ws]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)))

(defonce embed-pg (atom nil))

(defn embedded-postgres!
  [config]
  (let [pg (reset! embed-pg (.start (EmbeddedPostgres/builder)))
        pg-port (.getPort pg)
        init-sql (slurp "./Docker/init.sql")
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port

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
  (try
    (-> (config/env)
        embedded-postgres!
        migrate!
        acl/system)
    (f)
    (finally
      (.close @embed-pg)
      (ws/stop))))

