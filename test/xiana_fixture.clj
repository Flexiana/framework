(ns xiana-fixture
  (:require
    [clj-test-containers.core :as tc]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.commons :refer [rename-key]]
    [xiana.config :as config]
    [xiana.db :as db-core]
    [xiana.rbac :as rbac]
    [xiana.route :as routes]
    [xiana.session :as session-backend]
    [xiana.sse :as sse]
    [xiana.webserver :as ws]))

(defn docker-postgres!
  [{pg-config :xiana/postgresql :as config}]
  (let [{:keys [dbname user password image-name]} pg-config
        container (tc/start!
                    (tc/create
                      {:image-name    image-name
                       :exposed-ports [5432]
                       :env-vars      {"POSTGRES_DB"       dbname
                                       "POSTGRES_USER"     user
                                       "POSTGRES_PASSWORD" password}}))

        port (get (:mapped-ports container) 5432)
        pg-config (assoc
                    pg-config
                    :port port
                    :embedded container
                    :subname (str "//localhost:" port "/" dbname))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (assoc config :xiana/postgresql pg-config)))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      (rename-key :xiana/auth :auth)
      docker-postgres!
      db-core/connect
      db-core/migrate!
      session-backend/init-backend
      routes/reset
      rbac/init
      sse/init
      ws/start
      closeable-map))

(defn std-system-fixture
  [config f]
  (with-open [_ (->system config)]
    (f)))
