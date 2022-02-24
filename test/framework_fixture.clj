(ns framework-fixture
  (:require
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.commons :refer [rename-key]]
    [xiana.config :as config]
    [xiana.db :as db-core]
    [xiana.rbac :as rbac]
    [xiana.route :as routes]
    [xiana.session :as session-backend]
    [xiana.sse :as sse]
    [xiana.webserver :as ws]))

(defonce test-system (atom {}))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      (rename-key :xiana/auth :auth)
      db-core/docker-postgres!
      session-backend/init-backend
      db-core/connect
      db-core/migrate!
      routes/reset
      rbac/init
      sse/init
      ws/start
      closeable-map))

(defn std-system-fixture
  [config f]
  (with-open [_ (reset! test-system (->system config))]
    (f)))
