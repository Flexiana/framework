(ns xiana.websockets.router-helpers
  (:require
    [clojure.data.json :refer [read-str]]
    [clojure.edn :as edn]
    [clojure.string :as str]))

(defn string->
  "String to `uri`, uses the first word as action key"
  [s]
  (first (str/split s #"\s")))

(defn edn->
  "EDN to `uri`, converts edn string to map, extract :action key"
  [e]
  (:action (edn/read-string e)))

(defn json->
  "JSON to `uri`, converts json string to map, extract :action key"
  [j]
  (:action (read-str j :key-fn keyword)))

(defn probe->
  "Tries to solve the routing, in order:

   - json->
   - edn->
   - string->"
  [e]
  (name
    (or (try (json-> e)
             (catch Exception _ nil))
        (try (edn-> e)
             (catch Exception _ nil))
        (try (string-> e)
             (catch Exception _ nil)))))
