(ns framework.db.acl
  (:require
    [clojure.string :as str]))

(defn collify
  [x]
  (if (coll? x) x [x]))

(defn un-collify
  [x]
  (if (= 1 (count x)) (first x) x))

(defn insert-action
  ([actions table-actions]
   (reduce (fn [acc {:keys [table actions]}]
             (insert-action acc table actions))
     actions
     table-actions))
  ([actions table action]
   (let [t (cond-> (collify table)
             coll? first
             keyword? name)
         table-actions (first (filter #(#{t} (:table %)) actions))
         old-actions (into #{} (:actions table-actions))]
     (conj (remove #(= table-actions %) actions)
       {:table t :actions (reduce conj old-actions (collify action))}))))

(defn str->roles
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

(defn map->roles
  ([query]
   (map->roles query []))
  ([query actions]
   (println (mapv (partial map->roles actions) (filter map? (:where query))))
   (cond-> actions
     (:select query) (insert-action (:from query) :select)
     (:delete-from query) (insert-action (:delete-from query) :delete)
     (:where query) (insert-action (flatten (mapv map->roles (filter map? (:where query))))))))

;(insert-action [] [:producers] :select)

(defn ->roles
  [query]
  (if (string? query)
    (map #(update % :actions vec) (str->roles query))
    (map #(update % :actions vec) (map->roles query))))

(defn ->where
  [query]
  (last (flatten (re-seq #"WHERE (user-id|id|user\.id) (EQ|=) ([\w-]+)" query))))

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
                          roles-at-table (first (filter #(or (= :all (:table %)) (= table (:table %))) roles))
                          actions-at-table (:actions roles-at-table)
                          filter-at-table (:filter roles-at-table)]
                      (cond
                        (= :all filter-at-table) (every? (set actions-at-table) actions)
                        (= :own filter-at-table) (and (owns? query (get-in session [:user :id])) (every? (set actions-at-table) actions))
                        :else false))))))
