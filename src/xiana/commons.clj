(ns xiana.commons)

(def x-name (comp keyword name))
(def x-namespace (comp keyword namespace))

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
  [m from to]
  (let [v (if (qualified-keyword? from)
            (get-in m [(x-namespace from) (x-name from)])
            (get m from))]
    (assoc m to v)))

(defn deep-merge [& maps]
  (apply merge-with
         (fn [& args]
           (if (every? map? args)
             (apply deep-merge args)
             (last args)))
         maps))
