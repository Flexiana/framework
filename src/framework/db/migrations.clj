(ns framework.db.migrations
  (:require [framework.config.core :as config]
            [framework.db.impl.migrations.core :as impl.core]
            [migratus.core :as migratus]))

(defn migration-cfg
  [config]
  (let [pg-cfg (:framework.db.storage/postgresql config)
        mig-cfg (-> config
                    :framework.db.storage/migration
                    (assoc :db pg-cfg))]
    mig-cfg))

(defn create
  [name]
  (impl.core/create (migration-cfg (config/edn)) name))

(defn migrate
  []
  (migratus/migrate (migration-cfg (config/edn))))

(defn rollback-last
  []
  (migratus/rollback (migration-cfg (config/edn))))

(defn reset
  []
  (migratus/reset (migration-cfg (config/edn))))
