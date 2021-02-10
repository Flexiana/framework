(ns framework.config.core
  (:require
    [com.stuartsierra.component :as component]
    [config.core :refer [load-env]]))

(defrecord Config []
  component/Lifecycle
  (start [this]
         (load-env)))

(defn new-config
  []
  (->Config))

(defn edn
  []
  (load-env))
