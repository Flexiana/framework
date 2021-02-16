(ns framework.db.acl
  (:require
    [clojure.string :as str]))

(defn insert-action
  [actions table action]
  (let [table-actions (first (filter #(#{table} (:table %)) actions))
        old-actions (into #{} (:actions table-actions))]
    (conj (remove #(= table-actions %) actions)
      {:table table :actions (conj old-actions action)})))

(defn ->roles
  [query]
  (let [q (str/replace query ";" " ")
        r (reduce
            (fn [acc [_ action _ table]]
              (insert-action acc table (keyword (str/lower-case action))))
            []
            (or
              (re-seq #"(ALTER|SELECT|INSERT|CREATE|DROP|TRUNCATE|DELETE).*(FROM|INTO|TABLE)\s+(\S*)" q)
              (re-seq #"(UPDATE|TRUNCATE)(\s+)(\S*)" q)))]
    (map #(update % :actions vec) r)))

(defn ->where
  [query]
  (last (flatten (re-seq #"WHERE (user-id|id) EQ ([\w-]+)" query))))

(defn owns?
  [query user-id]
  (= (str user-id) (->where query)))

(defn acl
  [{:keys [session]} query]
  (let [action (->roles query)
        roles (get-in session [:user :roles])]
    (every? true? (for [a action]
                    (let [table (:table a)
                          actions (:actions a)
                          roles-in-table (first (filter #(or (= :all (:table %)) (= table (:table %))) roles))
                          actions-in-table (:actions roles-in-table)
                          filer-in-table (:filter roles-in-table)]
                      (cond
                        (= :all filer-in-table) (every? (set actions-in-table) actions)
                        (= :own filer-in-table) (and (owns? query (get-in session [:user :id])) (every? (set actions-in-table) actions))
                        :else false))))))
