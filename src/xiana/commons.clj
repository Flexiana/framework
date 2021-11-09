(ns xiana.commons)

(defn ?assoc-in
  "Same as assoc-in, but skip the assoc if v is nil"
  [m [k & ks] v]
  (if v
    (if ks
      (assoc m k (assoc-in (get m k) ks v))
      (assoc m k v))
    m))

(defn map-keys
  [f m]
  (zipmap (map f (keys m))
          (vals m)))

(defn rename-key
  [config from to]
  (-> config
      (assoc to (get config from))
      (dissoc from)))
