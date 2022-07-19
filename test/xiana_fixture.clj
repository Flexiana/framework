(ns xiana-fixture
  (:require
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [utils]
    [xiana.commons :refer [rename-key]]
    [xiana.config :as config]
    [xiana.db :as db-core]
    [xiana.rbac :as rbac]
    [xiana.route :as routes]
    [xiana.session :as session-backend]
    [xiana.sse :as sse]
    [xiana.webserver :as ws]))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      (rename-key :xiana/auth :auth)
      utils/docker-postgres!
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
