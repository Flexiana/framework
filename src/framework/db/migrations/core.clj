(ns framework.db.migrations.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [migratus.core :as migratus]
    [migratus.migrations :as migrations]
    [migratus.protocols :as proto]
    [migratus.utils :as utils])
  (:import
    java.text.SimpleDateFormat
    (java.util
      Date
      TimeZone)))

(def content-migratus-file
  "{:ns %s\n :up-fn up\n :down-fn down}")

(def content-clojure-file
  "(ns %s\n  (:require [framework.db.sql :as sql]))")

(def migrations-folder-path
  (-> (java.io.File. "src/framework/db/migration_files") .getAbsolutePath))

(defn create-clojure-file
  [fpath ^String namespace]
  (with-open [wrtr (io/writer fpath)]
    (.write wrtr namespace)
    (.write wrtr "\n\n")
    (.write wrtr "(defn up [config])")
    (.write wrtr "\n\n")
    (.write wrtr "(defn down [config])")))

(defn insert-content-migratus-file
  [mdir mname mns]
  (let [file (io/file mdir (str mname ".edn"))]
    (spit file (format content-migratus-file mns))))

(defn timestamp
  []
  (let [fmt (doto (SimpleDateFormat. "yyyyMMddHHmmss ")
              (.setTimeZone (TimeZone/getTimeZone "UTC")))]
    (.format fmt (Date.))))

(defn migratus-create
  [env name]
  (let [migration-dir  (migrations/find-or-create-migration-dir
                         (utils/get-parent-migration-dir env)
                         (utils/get-migration-dir env))
        migration-name (migrations/->kebab-case (str (timestamp) name))]
    (doseq [mig-file (proto/migration-files* :edn migration-name)]
      (.createNewFile (io/file migration-dir mig-file)))
    [migration-name migration-dir]))

(defn create
  [env name]
  (let [[migration-name migration-dir] (migratus-create env name)
        filename                       (string/replace migration-name #"-" "_")
        absolute-path                  (str migrations-folder-path "/" filename ".clj")
        namespace                      (str "framework.db.migration-files." migration-name)
        namespace-content              (format content-clojure-file namespace)]
    (create-clojure-file absolute-path namespace-content)
    (insert-content-migratus-file migration-dir migration-name namespace)))

(defn migration-cfg
  [env]
  (let [{:framework.db.storage/keys [postgresql]} env
        mig-cfg                                   (assoc-in env [:framework.db.storage/migration
                                                                 :db]
                                                    postgresql)]
    mig-cfg))

(defn migrate
  [env]
  (->  env
       migration-cfg
       migratus/migrate))

(defn rollback-last
  [env]
  (-> env
      migration-cfg
      migratus/rollback))

(defn reset
  [env]
  (-> env
      migration-cfg
      migratus/reset))
