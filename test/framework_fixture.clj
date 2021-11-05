(ns framework-fixture
  (:require
    [framework.config.core :as config]
    [framework.db.core :as db.core]
    [framework.db.test-support :as test-support]
    [framework.route.core :as routes]
    [framework.session.core :as session-backend]
    [framework.webserver.core :as ws]
    [framework.app :as-alias app]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]))

(defn ->system
  [{:keys      [controller-interceptors
                session-backend
                routes
                role-set
                router-interceptors
                web-socket-interceptors]
    ::app/keys [auth]
    :as        config
    :or        {session-backend (session-backend/init-in-memory)}}]
  (let [{:keys [datasource]} (db.core/start config)
        {:keys [web-server]} (ws/start config)]
   {:routes                  (routes/reset routes)
    :role-set                role-set
    :auth                    auth
    :session-backend         session-backend
    :router-interceptors     router-interceptors
    :web-socket-interceptors web-socket-interceptors
    :controller-interceptors controller-interceptors
    :db datasource
    :web-server web-server}))

(defn std-system-fixture
  [config f]
  (with-open [system (-> config/config
                         (merge config)
                         (test-support/docker-postgres! [(slurp "docker/sql-scripts/init.sql")])
                         #_(test-support/embedded-postgres! [(slurp "docker/sql-scripts/init.sql")])
                         test-support/migrate!
                         ->system
                         closeable-map)]
    (f)))