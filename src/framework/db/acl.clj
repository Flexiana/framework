(ns framework.db.acl
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(defn collify
  [x]
  (if (coll? x) x (vector x)))

(defn un-collify
  [x]
  (if (= 1 (count x)) (first x) x))

(defn ->table-name
  [table]
  (cond->
    (flatten (collify table))
    coll? first
    keyword? name))

(defn insert-action
  ([actions table-actions]
   (reduce (fn [acc {:keys [table actions]}]
             (insert-action acc table actions))
     actions
     table-actions))
  ([actions table action]
   (let [t (->table-name table)
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
   (cond-> actions
     (:select query) (insert-action (:from query) :select)
     (:update query) (insert-action (:update query) :update)
     (:truncate query) (insert-action (:truncate query) :truncate)
     (:insert-into query) (insert-action (:insert-into query) :insert)
     (:delete query) (insert-action (:delete query) :delete)
     (:delete-from query) (insert-action (:delete-from query) :delete)
     (:where query) (insert-action (flatten (mapv map->roles (filter map? (:where query)))))
     (:any query) (insert-action (flatten (mapv map->roles (filter map? (:any query)))))
     (:all query) (insert-action (flatten (mapv map->roles (filter map? (:all query)))))
     (:in query) (insert-action (flatten (mapv map->roles (filter map? (:in query)))))
     (:not-in query) (insert-action (flatten (mapv map->roles (filter map? (:not-in query)))))
     (:from query) (insert-action (flatten (mapv map->roles (filter map? (:from query)))))
     (:having query) (insert-action (flatten (mapv map->roles (filter map? (:having query))))))))

(defn ->roles
  [query]
  (map #(update % :actions vec)
       (cond
         (string? query) (str->roles query)
         (map? query) (map->roles query)
         (coll? query) (flatten (map ->roles query)))))

(defn str->where
  [query]
  (last (flatten (re-seq #"WHERE (user-id|id|user\.id) (EQ|=) ([\w-]+)" query))))

(defn column-aliases
  [{:keys [select]}]
  (reduce (fn [acc q]
            (cond
              (keyword? q) (conj acc (into [] (repeat 2 (->table-name q))))
              (vector? q) (conj acc (into [] (map ->table-name (reverse q))))
              :else acc))
    {} select))

(defn table-aliases
  [{:keys [from join left-join right-join full-join cross-join]}]
  (reduce (fn [acc fq]
            (cond
              (string? fq) (conj acc (into [] (repeat 2 fq)))
              (keyword? fq) (conj acc (into [] (repeat 2 (->table-name fq))))
              (coll? fq) (conj acc (into [] (map ->table-name (reverse fq))))
              :else acc))
    {}
    (reduce conj (take-nth 2 (concat join left-join right-join full-join cross-join)) from)))

(defn map->where-collect
  [{:keys [update delete-from insert-into from where] :as query}]
  (let [[op p1 p2] where
        t-name (or insert-into from update delete-from)
        t-aliases (table-aliases query)
        c-aliases (column-aliases query)
        c-name #(get c-aliases % %)
        full-field-name #(let [[c t] (reverse (str/split (->table-name %) #"\."))]
                           (if
                             t (format "%s.%s" (t-aliases t) (c-name c))
                             (format "%s.%s" (->table-name t-name) (c-name c))))]
    (cond-> []
      (vector? p1) (concat (map->where-collect (assoc query :where p1)))
      (vector? p2) (concat (map->where-collect (assoc query :where p2)))
      (keyword? p1) (conj {:op op (full-field-name p1) p2})
      (keyword? p2) (conj {:op op (full-field-name p2) p1}))))

(defn map->owns?
  [query user-id]
  (let [roles (->>
                (map->where-collect query)
                (filter #(get % "users.id"))
                (map #(and (= := (:op %)) (= user-id (get % "users.id")))))]
    (and (not-empty roles) (every? true? roles))))

(defn owns?
  [query user-id]
  (cond
    (string? query) (= (str user-id) (str->where query))
    (map? query) (map->owns? query user-id)
    (coll? query) (every? true? (flatten (map #(owns? % user-id) query)))))

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
