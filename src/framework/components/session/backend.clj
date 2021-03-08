(ns framework.components.session.backend
  (:require
    [com.stuartsierra.component :as component]))

(defprotocol SessionStore
  (fetch
    [store key])
  (add!
    [store key value])
  (delete!
    [store key])
  (dump [store]))

(defn init-in-memory-session
  ([] (init-in-memory-session (atom {})))
  ([store]
   (reify SessionStore
     (fetch [_ k]
       (get @store k))
     (add! [_ k v]
       (let [key (or k (java.util.UUID/randomUUID))]
         (swap! store assoc key v)))
     (delete! [_ k]
       (swap! store dissoc k))
     (dump [_]
       @store))))

(defrecord SessionBackend
  []
  component/Lifecycle
  (stop [this] this)
  (start
    [this]
    (assoc this :session-backend (init-in-memory-session))))

(defn make-session-store
  []
  (map->SessionBackend {}))
