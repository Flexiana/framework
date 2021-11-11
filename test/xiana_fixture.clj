(ns xiana-fixture
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

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      (rename-key :xiana/auth :auth)
      session-backend/init-in-memory
      db-core/start
      db-core/migrate!
      routes/reset
      rbac/init
      sse/init
      ws/start
      (select-keys [:auth
                    :session-backend
                    :db
                    :routes
                    :role-set
                    :events-channel
                    :controller-interceptors
                    :web-socket-interceptors
                    :router-interceptors
                    :webserver])
      closeable-map))

(defn std-system-fixture
  [config f]
  (with-open [_ (->system config)]
    (f)))

(defonce ttest (atom nil))

(comment

  (reset! ttest (->system (config/config)))

  (.close @ttest))
