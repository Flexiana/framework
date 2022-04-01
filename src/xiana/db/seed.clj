(ns xiana.db.seed
  (:require
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.commons :refer [rename-key]]
    [xiana.config :as config]))

(defn seed!
  ([config]
   (let [db-conf (if-let [datasource (some-> (get-in config [:xiana/postgresql :datasource])
                                             jdbc/get-datasource)]
                   {:datasource datasource}
                   (:xiana/postgresql config))
         mig-config (-> (assoc (:xiana/migration config) :db db-conf)
                        (rename-key :seeds-dir :migration-dir)
                        (rename-key :seeds-table-name :migration-table-name))]
     (migratus/migrate mig-config))
   config))

(def seed-config
  (let [config (config/config)
        db-conf (:xiana/postgresql config)]
    (-> (assoc (:xiana/migration config) :db db-conf)
        (rename-key :seeds-dir :migration-dir)
        (rename-key :seeds-table-name :migration-table-name))))

(defn -main [& args]
  (let [[command name type] args]
    (if (and (:migration-dir seed-config) (:migration-table-name seed-config))
      (case command
        "create" (migratus/create seed-config name (keyword type))
        "reset" (migratus/reset seed-config)
        "destroy" (migratus/destroy seed-config)
        "migrate" (migratus/migrate seed-config)
        (println "You can 'create' 'reset' 'destroy' or 'migrate' your seed data"))
      (println "No seed configuration found"))))
