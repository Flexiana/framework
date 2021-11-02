(ns framework-fixture
  (:require
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.db.test-support :as test-support]
    [framework.route.core :as routes]
    [framework.session.core :as session-backend]
    [framework.webserver.core :as ws]))

(defn system
  [config]
  (let [session-backend (:session-backend config (session-backend/init-in-memory))
        deps {:webserver               (:framework.app/web-server config)
              :routes                  (routes/reset (:routes config))
              :role-set                (:role-set config)
              :auth                    (:framework.app/auth config)
              :session-backend         session-backend
              :router-interceptors     (:router-interceptors config)
              :web-socket-interceptors (:web-socket-interceptors config)
              :controller-interceptors (:controller-interceptors config)
              :db                      (db-core/start
                                         (:framework.db.storage/postgresql config))}]
    (update deps :webserver (ws/start deps))))

(defn std-system-fixture
  [config f]
  (try
    (-> (merge (config/env) config)
        ;(test-support/docker-postgres! [(slurp "docker/sql-scripts/init.sql")])
        (test-support/embedded-postgres! [(slurp "docker/sql-scripts/init.sql")])
        test-support/migrate!
        system)
    (f)
    (finally
      (ws/stop))))
