(ns framework.components.session.core)

(defprotocol Session
  (fetch?
    [store key])
  (add!
    [store key value])
  (delete!
    [store key]))

(defn init-in-memory-session
  ([] (init-in-memory-session (atom {})))
  ([store]
   (reify Session
     (fetch? [_ k]
       (get @store k))
     (add! [_ k v]
       (let [key (or k (java.util.UUID/randomUUID))]
         (swap! store assoc key v)))
     (delete! [_ k]
       (swap! store dissoc k)))))
