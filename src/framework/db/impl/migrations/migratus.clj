(ns framework.db.impl.migrations.migratus
  (:require [migratus.migrations :as migrations]
            [migratus.protocols :as proto]
            [migratus.utils :as utils]
            [clojure.java.io :as io])
  (:import [java.util Date TimeZone]
           java.text.SimpleDateFormat))

(defn timestamp []
  (let [fmt (doto (SimpleDateFormat. "yyyyMMddHHmmss ")
              (.setTimeZone (TimeZone/getTimeZone "UTC")))]
    (.format fmt (Date.))))

(defn create
  [config name]
  (let [migration-dir  (migrations/find-or-create-migration-dir
                         (utils/get-parent-migration-dir config)
                         (utils/get-migration-dir config))
        migration-name (migrations/->kebab-case (str (timestamp) name))]
    (doseq [mig-file (proto/migration-files* :edn migration-name)]
      (.createNewFile (io/file migration-dir mig-file)))
    [migration-name migration-dir]))

(defn get-migrations-folder-path
  [config]
  (-> (migrations/find-or-create-migration-dir
        (utils/get-parent-migration-dir config)
        (utils/get-migration-dir config))
      (.getAbsolutePath)))
