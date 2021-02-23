(ns framework.db.acl
  (:require
    [clojure.string :as str]))

(defn- collify
  [x]
  (if (coll? x) x (vector x)))

(defn- ->table-name
  [table]
  (cond->
    (flatten (collify table))
    coll? first
    keyword? name))

(defn insert-action
  ([actions table-actions]
   (reduce (fn [acc {:keys [table actions]}] (insert-action acc table actions))
     actions
     table-actions))
  ([actions table action]
   (let [t (->table-name table)
         table-actions (first (filter #(#{t} (:table %)) actions))
         old-actions (into #{} (:actions table-actions))]
     (conj (remove #(= table-actions %) actions)
       {:table t :actions (reduce conj old-actions (collify action))}))))

(defn- str->necessary-permits
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

(defn- sql-map->necessary-permits
  ([query]
   (sql-map->necessary-permits query []))
  ([query actions]
   (cond-> actions
     (:select query) (insert-action (:from query) :select)
     (:update query) (insert-action (:update query) :update)
     (:truncate query) (insert-action (:truncate query) :truncate)
     (:insert-into query) (insert-action (:insert-into query) :insert)
     (:delete query) (insert-action (:delete query) :delete)
     (:delete-from query) (insert-action (:delete-from query) :delete)
     (:where query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:where query)))))
     (:any query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:any query)))))
     (:all query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:all query)))))
     (:in query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:in query)))))
     (:not-in query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:not-in query)))))
     (:from query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:from query)))))
     (:having query) (insert-action (flatten (mapv sql-map->necessary-permits (filter map? (:having query))))))))

(defn ->necessary-permits
  [query]
  (map #(update % :actions vec)
       (cond
         (string? query) (str->necessary-permits query)
         (map? query) (sql-map->necessary-permits query)
         (coll? query) (flatten (map ->necessary-permits query)))))

(defn- str->where
  [query]
  (->> (re-seq #"WHERE (user-id|id|users\.id) (EQ|=) ([\w-]+)" query)
       flatten
       last))

(defn reverse-column-aliases
  [{:keys [select]}]
  (reduce (fn [acc q]
            (cond
              (keyword? q) (conj acc (into [] (repeat 2 (->table-name q))))
              (vector? q) (conj acc (into [] (map ->table-name (reverse q))))
              :else acc))
    {} select))

(defn reverse-table-aliases
  [{:keys [from join left-join right-join full-join cross-join]}]
  (let [join-tables (->>
                      (concat join left-join right-join full-join cross-join)
                      (take-nth 2))
        tables (concat join-tables from)]
    (reduce (fn [acc fq]
              (cond
                (string? fq) (conj acc (into [] (repeat 2 fq)))
                (keyword? fq) (conj acc (into [] (repeat 2 (->table-name fq))))
                (coll? fq) (conj acc (into [] (map ->table-name (reverse fq))))
                :else acc))
      {}
      tables)))

(defn- full-field-name
  [{:keys [update delete-from insert-into from] :as query} field]
  (let [t-name (or insert-into from update delete-from)
        t-aliases (reverse-table-aliases query)
        c-aliases (reverse-column-aliases query)
        [c t] (reverse (str/split (->table-name field) #"\."))
        c-name (get c-aliases c c)]
    (if t
      (format "%s.%s" (t-aliases t) c-name)
      (format "%s.%s" (->table-name t-name) c-name))))

(defn sql-map->wheres
  [{:keys [where] :as query}]
  (let [[op p1 p2] where]
    (cond-> []
      (vector? p1) (concat (sql-map->wheres (assoc query :where p1)))
      (vector? p2) (concat (sql-map->wheres (assoc query :where p2)))
      (keyword? p1) (conj {:op op (full-field-name query p1) p2})
      (keyword? p2) (conj {:op op (full-field-name query p2) p1}))))

(defn sql-map->owns?
  [query user-id]
  (let [roles (->>
                (sql-map->wheres query)
                (filter #(get % "users.id"))
                (map #(and (= := (:op %)) (= user-id (get % "users.id")))))]
    (and (not-empty roles) (every? true? roles))))

(defn owns?
  [query user-id]
  (cond
    (string? query) (= (str user-id) (str->where query))
    (map? query) (sql-map->owns? query user-id)
    (coll? query) (every? true? (flatten (map #(owns? % user-id) query)))))

(defn acl
  [{:keys [session]} query]
  (let [necessary-permits (->necessary-permits query)
        existing-permits (get-in session [:user :roles])]
    (every? true? (for [{:keys [actions table]} necessary-permits]
                    (let [existing-permissions-on-table (->> existing-permits
                                                             (filter #(or (= :all (:table %)) (= table (:table %))))
                                                             first)
                          actions-on-table (:actions existing-permissions-on-table)
                          filter-on-actions (:filter existing-permissions-on-table)]
                      (cond
                        (= :all filter-on-actions) (or (= :all actions-on-table) (every? (set actions-on-table) actions))
                        (= :own filter-on-actions) (and (owns? query (get-in session [:user :id])) (every? (set actions-on-table) actions))
                        :else false))))))
