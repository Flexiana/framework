(ns framework.config.core
  "Solves environment and config variables"
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [config.core :refer [load-env]]
    [xiana.commons :refer [deep-merge]])
  (:import
    (java.io
      PushbackReader)))

(defn read-edn-file
  "Reads edn configuration file."
  [config]
  (if-let [edn-file (:framework-edn-config config)]
    (with-open [r (io/reader edn-file)]
      (deep-merge config (edn/read (PushbackReader. r))))
    config))

(defn config
  "Returns a new config instance.

  You can pass path to the config file with the `:framework-edn-config` key.
  It's useful for choosing an environment different from the current one
  (e.g. `test` or `production` while in the `dev` repl)."
  ([]
   (config {}))
  ([config]
   (-> (load-env)
       (deep-merge config)
       read-edn-file)))
