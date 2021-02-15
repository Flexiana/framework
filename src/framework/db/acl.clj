(ns framework.db.acl
  (:require [clojure.string :as str]))


(defn action [query]
  (let [q (str/replace query ";" " ")
        r (map
            (fn [[_ action _ on]] [(keyword (str/lower-case action)) on])
            (or
              (re-seq #"(ALTER|SELECT|INSERT|CREATE|DROP|TRUNCATE|DELETE).*(FROM|INTO|TABLE)\s+(\S*)" q)
              (re-seq #"(UPDATE|TRUNCATE)(\s+)(\S*)" q)))]
    r))

(defn fetch [session key]
  (get session key ""))


(defn user-id [env]
  (:id (fetch (:session env) :user)))

