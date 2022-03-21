(ns framework.db.seed
  (:require
    [migratus.core :as migratus]
    [next.jdbc :as jdbc]
    [xiana.commons :refer [rename-key]]))

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
