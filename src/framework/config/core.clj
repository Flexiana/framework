(ns framework.config.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

;; default edn file
(def default-edn-file
  (.getAbsolutePath (java.io.File. "config/dev/config.edn")))

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
  (with-open [r (io/reader (or edn-file default-edn-file))]
    (edn/read (java.io.PushbackReader. r))))

(defn get-spec
  "Select configuration spec using 'k' identifier."
  ([k] (get-spec k nil))
  ([k edn-file]
   (get (read-edn-file edn-file)
        (-> k default-config-map))))
