(ns framework.db.core
  (:require
   [next.jdbc :as jdbc]
   [framework.config.core :as config]))

;; database instance reference
(defonce db (atom {}))

(defn- make
  "Return database instance map."
  [spec]
  (let [datasource (try (jdbc/get-datasource spec) (catch Exception _ nil))]
    {:datasource datasource
     :connection (try
                   (jdbc/get-connection datasource)
                   (catch Exception _ nil))}))

(defn start
  "Start database instance.
  Get the database specification from
  the 'edn' configuration file in case of
  db-spec isn't set."
  ([] (start nil))
  ([db-spec]
   (when-let [spec (or db-spec (config/get-spec :database))]
     (swap! db
            (fn [m]
              (merge m (make spec)))))))

(defn connection
  "Get (or start) database connection.
  Start the database instance if necessary (not cached).
  Return the connection or nil which means that was not possible
  to establish a proper link."
  []
  (when (or
         ;; empty structure reference?
         (empty @db)
         ;; not connected?
         (not (:connection @db)))
    ;; tries to start the database connection
    (start))
  ;; connection not established
  (:connection @db))
