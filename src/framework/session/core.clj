(ns framework.session.core
  (:import (java.util UUID)))

;; define session protocol
(defprotocol Session
  ;; fetch an element (no side effect)
  (fetch [_ k])
  (dump [_])
  ;; add an element (side effect)
  (add! [_ k v])
  ;; delete an element (side effect)
  (delete! [_ k])
  ;; erase all elements (side effect)
  (erase! [_]))

(defn init-in-memory
  "Initialize session in memory."
  ([] (init-in-memory (atom {})))
  ([m]
   ;; implement the Session protocol
   (reify Session
     ;; fetch session key:element
     (fetch [_ k] (get @m k))
     (dump [_] @m)
     ;; add session key:element
     (add! [_ k v]
       (let [k (or k (UUID/randomUUID))]
         (swap! m assoc k v)))
     ;; delete session key:element
     (delete! [_ k] (swap! m dissoc k))
     ;; erase session
     (erase! [_] (reset! m {})))))
