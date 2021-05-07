(ns framework.config.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

;; set configuration environment variable name
(def env-edn-file "FRAMEWORK_EDN_CONFIG")

;; set default edn file
(def default-edn-file
  (when-let [edn-file (System/getenv env-edn-file)]
    (.getAbsolutePath (java.io.File. edn-file))))

;; default config map: util wrapper/translator
(def default-config-map
  {:acl       :framework.app/acl
   :auth      :framework.app/auth
   :emails    :framework.app/emails
   :webserver :framework.app/web-server
   :migration :framework.db.storage/migration
   :database  :framework.db.storage/postgresql})

(defn read-edn-file
  "Read edn configuration file."
  [edn-file]
  (when-let [edn-file (or edn-file default-edn-file)]
    (with-open [r (io/reader edn-file)]
      (edn/read (java.io.PushbackReader. r)))))

(defn get-spec
  "Select configuration spec using 'k' identifier."
  ([k] (get-spec k nil))
  ([k edn-file]
   (get (read-edn-file edn-file)
        (-> k default-config-map))))
