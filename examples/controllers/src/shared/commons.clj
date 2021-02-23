(ns commons)

;; TODO Q: Place global helpers we want to use across the project. Avoid any ties to domain logic here. Good place for sth like extension to clojure.core.
;; I suggest this to be required :refer :all

(defn ?assoc-in
  "Same as assoc-in, but skip the assoc if v is nil"
  [m [k & ks] v]
  (if v
    (if ks
      (assoc m k (assoc-in (get m k) ks v))
      (assoc m k v))
    m))
