(ns xiana.config
  "Solves environment and config variables"
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [config.core :refer [load-env]]
    [xiana.commons :refer [deep-merge]])
  (:import
    (java.io
      PushbackReader)))

(defn read-edn-file
  "Reads edn configuration file."
  [config]
  (if-let [edn-file (:xiana-config config)]
    (with-open [r (io/reader edn-file)]
      (deep-merge config (edn/read (PushbackReader. r))))
    config))

(defn key-and-default [v]
  (let [[key-name default] (str/split v #"\|")]
    [(keyword (subs (str/trim key-name) 1)) (some-> default str/trim)]))

(defn- inject-env-vars
  [config]
  (into {} (map (fn mapper [[k v]]
                  (cond
                    (map? v) [k (into {} (map mapper v))]
                    (and (string? v) (str/starts-with? v "$")) (let [[_key default] (key-and-default v)]
                                                                 [k (get config _key default)])
                    :else [k v]))
                config)))

(defn config
  "Returns a new config instance.

  You can pass path to the config file with the `:xiana-config` key.
  It's useful for choosing an environment different from the current one
  (e.g. `test` or `production` while in the `dev` repl)."
  ([]
   (config {}))
  ([config]
   (-> (load-env)
       (deep-merge config)
       read-edn-file
       inject-env-vars)))
