(ns framework.db.impl.migrations.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [framework.db.impl.migrations.migratus :as impl.migratus]))

(def content-migratus-file
  "{:ns %s\n :up-fn up\n :down-fn down}")

(def content-clojure-file
  "(ns %s\n  (:require [framework.db.sql :as sql]))")

(defn create-clojure-file
  [fpath namespace]
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

(defn create
  [config name]
  (let [[migration-name migration-dir] (impl.migratus/create config name)
        filename               (string/replace migration-name #"-" "_")
        migrations-folder-path (impl.migratus/get-migrations-folder-path config)
        absolute-path          (str migrations-folder-path "/" filename ".clj")
        namespace              (str "framework.db.migration-files." migration-name)
        namespace-content      (format content-clojure-file namespace)]
    (create-clojure-file absolute-path namespace-content)
    (insert-content-migratus-file migration-dir migration-name namespace)))
