(ns framework.db.acl
  (:require [clojure.string :as str]))


(defn action-table [query]
  (let [q (str/replace query ";" " ")
        r (map
            (fn [[_ action _ on]] [(keyword (str/lower-case action)) on])
            (or
              (re-seq #"(ALTER|SELECT|INSERT|CREATE|DROP|TRUNCATE|DELETE).*(FROM|INTO|TABLE)\s+(\S*)" q)
              (re-seq #"(UPDATE|TRUNCATE)(\s+)(\S*)" q)))]
    r))

(defn acl [{:keys [session]} query]
  (let [action (action-table query)
        roles (first (vals (get-in session [:user :roles])))
        reduced (->> (for [a action
                           r roles]
                       (when (= (second a) (get r (first a))) r))
                     (remove nil?))]
    (when (= (count action) (count reduced))
      reduced)))

