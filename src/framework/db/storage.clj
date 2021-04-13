(ns framework.db.storage
  (:require
    [com.stuartsierra.component :as component]
    [next.jdbc :as jdbc]))

;; DEPRECATED
(defrecord PostgreSQL
  [config]
  component/Lifecycle
  (start [this]
         (let [datasource (jdbc/get-datasource config)]
           (-> this
               (assoc :datasource datasource)
               (assoc :connection (jdbc/get-connection datasource)))))
  (stop [this]
        (dissoc this :datasource :connection)))

(defn ->postgresql
  [{pg-cfg :framework.db.storage/postgresql}]
  (with-meta pg-cfg
    `{component/start ~(fn [this]
                         (let [datasource (jdbc/get-datasource pg-cfg)]
                           (-> this
                               (assoc :datasource datasource)
                               (assoc :connection (jdbc/get-connection datasource)))))
      component/stop  ~(fn [{:keys [connection]
                             :as   this}]
                         (.close connection)
                         (dissoc this :datasource :connection))}))
