(ns framework.config.core
  (:require [config.core :refer [load-env]]
            [com.stuartsierra.component :as component]))

(defrecord Config []
  component/Lifecycle
  (start [this]
    (load-env)))

(defn new-config []
  (->Config))

(defn edn
  []
  (load-env))
