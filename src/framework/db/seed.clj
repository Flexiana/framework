(ns framework.db.seed
  (:require
    [framework.config.core :as config]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.commons :refer [rename-key]]))

(defn seed!
  ([config]
   (let [db-conf (if-let [datasource (some-> (get-in config [:framework.db.storage/postgresql :datasource])
                                             jdbc/get-datasource)]
                   {:datasource datasource}
                   (:framework.db.storage/postgresql config))
         mig-config (-> (assoc (:framework.db.storage/migration config) :db db-conf)
                        (rename-key :seeds-dir :migration-dir)
                        (rename-key :seeds-table-name :migration-table-name))]
     (migratus/migrate mig-config))
   config))

(def seed-config
  (let [config (config/config)
        db-conf (:framework.db.storage/postgresql config)]
    (-> (assoc (:framework.db.storage/migration config) :db db-conf)
        (rename-key :seeds-dir :migration-dir)
        (rename-key :seeds-table-name :migration-table-name))))

(defn -main [& args]
  (if (and (:migration-dir seed-config) (:migration-table-name seed-config))
    (case (first args)
      "create" (migratus/create seed-config (second args))
      "reset" (migratus/reset seed-config)
      "destroy" (migratus/destroy seed-config)
      "migrate" (migratus/migrate seed-config)
      (println "You can 'create' 'reset' 'destroy' or 'migrate' your seed data"))
    (println "No seed configuration found")))
