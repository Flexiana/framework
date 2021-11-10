(ns framework-fixture
  (:require
    [framework.config.core :as config]
    [framework.db.core :as db-core]
    [framework.rbac.core :as rbac]
    [framework.route.core :as routes]
    [framework.session.core :as session-backend]
    [framework.sse.core :as sse]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.commons :refer [rename-key]]))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      (rename-key :framework.app/auth :auth)
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
                    :rbac
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
