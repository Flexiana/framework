(ns xiana.config
  "Solves environment and config variables"
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [config.core :refer [load-env]])
  (:import
    (java.io
      PushbackReader)))

(defn read-edn-file
  "Read edn configuration file."
  [config]
  (if-let [edn-file (:framework-edn-config config)]
    (with-open [r (io/reader edn-file)]
      (merge config (edn/read (PushbackReader. r))))
    config))

(defn config
  ([]
   (config {}))
  ([config]
   (-> (load-env)
       (merge config)
       read-edn-file)))
