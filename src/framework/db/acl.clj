(ns framework.db.acl
  (:require [clojure.string :as str]))

(defn insert-action
  [actions table action]
  (let [table-actions (first (filter #(#{table} (:table %)) actions))
        old-actions (into #{} (:actions table-actions))]
    (println table-actions)
    (conj (remove #(= table-actions %) actions) {:table table :actions (conj old-actions action)})))

(defn ->roles [query]
  (let [q (str/replace query ";" " ")
        r (reduce
            (fn [acc [_ action _ table]]
              (insert-action acc table (keyword (str/lower-case action))))
            []
            (or
              (re-seq #"(ALTER|SELECT|INSERT|CREATE|DROP|TRUNCATE|DELETE).*(FROM|INTO|TABLE)\s+(\S*)" q)
              (re-seq #"(UPDATE|TRUNCATE)(\s+)(\S*)" q)))]
    (map #(update % :actions vec) r)))

(defn acl [{:keys [session]} query]
  (let [action (->roles query)
        roles (vals (get-in session [:user :roles]))
        reduced (->> (for [a action
                           r roles]
                       (when (= (second a) (get r (first a))) r))
                     (remove nil?))]
    (println action)
    (println roles)
    reduced))

