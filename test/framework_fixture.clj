(ns framework-fixture
  (:require
    [clj-test-containers.core :as tc]
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.route.core :as routes]
    [framework.session.core :as session-backend]
    [framework.webserver.core :as ws]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]))

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
                                         (:database-connection config))}]
    (assoc deps :web-server (ws/start deps))))

(defn docker-postgres!
  [config]
  (let [container (-> (tc/create {:image-name    "postgres:11.5-alpine"
                                  :exposed-ports [5432]})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)

        db-config (-> config
                      :database-connection
                      (assoc
                        :port port
                        :dbtype "postgresql"
                        :dbname "framework_test"
                        :embedded container
                        :user "postgres"
                        :subname (str "//localhost:" port "/frankie")))]
    (jdbc/execute! (dissoc db-config :dbname) ["CREATE DATABASE framework_test;"])
    (jdbc/execute! db-config ["GRANT ALL PRIVILEGES ON DATABASE framework_test TO postgres;"])
    (assoc config :database-connection db-config)))

(defn migrate!
  [config]
  (let [db (:database-connection config)
        mig-config (assoc (:framework.db.storage/migration config) :db db)]
    (migratus/migrate mig-config))
  config)

(defn std-system-fixture
  [config f]
  (try
    (-> (config/env)
        (merge config)
        docker-postgres!
        (assoc-in [:framework.app/web-server :port] 3333)
        migrate!
        system)
    (f)
    (finally
      (ws/stop))))
