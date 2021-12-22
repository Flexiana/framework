(ns framework.config.core
  "Solves environment and config variables"
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [xiana.commons :refer [deep-merge x-name x-namespace]])
  (:import
    (java.io
      PushbackReader)))

(defn read-edn-file
  "Reads edn configuration file."
  [config]
  (if-let [edn-file (:framework-edn-config config)]
    (with-open [r (io/reader edn-file)]
      (merge config (edn/read (PushbackReader. r))))
    config))

(defn flat->deep
  [m]
  (let [qualified (filter #(qualified-keyword? (first %)) m)
        un-q (into {} (remove #(qualified-keyword? (first %)) m))
        gr (group-by (fn [[k _]] (x-namespace k)) qualified)]
    (into un-q (map (fn [[g vs]]
                      (let [ms (into {} (map (fn [[k v]] [(x-name k) v]) vs))]
                        [g ms]))
                    gr))))

(defn keywordize
  [m]
  (map (fn [[k v]]
         [(-> (str/lower-case k)
              (str/replace "__" "/")
              (str/replace "_" "-")
              (str/replace "." "-")
              keyword) v]) m))

(def sys-env
  (let [props (flat->deep (keywordize (System/getProperties)))
        envs (flat->deep (keywordize (System/getenv)))]
    (deep-merge props envs)))

(def config-file
  (some-> (io/resource "config.edn") slurp edn/read-string))

(defn- config*
  "Returns a new config instance.

  You can pass path to the config file with the `:framework-edn-config` key.
  It's useful for choosing an environment different from the current one
  (e.g. `test` or `production` while in the `dev` repl)."
  [& override]
  (let [deep-file (flat->deep (or config-file {}))
        override-map (let [m (first override)]
                       (cond
                         (map? m) m
                         (empty? m) {}
                         :else (apply hash-map override)))
        deep-override (flat->deep override-map)]
    (-> (deep-merge deep-file sys-env deep-override)
        read-edn-file)))

(def config
  "Customized config resolver for support both name-spaced and deep configuration maps"
  (memoize config*))
