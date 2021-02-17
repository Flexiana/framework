(ns router
  (:require
    [com.stuartsierra.component :as component]
    [controllers.home-ctrl :as hctrl]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [reitit.ring :as ring]
    [view.core :as xviews]
    [xiana.core :as xiana]
    [controllers.records-ctrl :as rctrl]))


(def routes
  [["/" {:controller hctrl/home-controller}]
   ["/records" {:controller rctrl/records-controller}]
   ["/re-frame" {:controller re-frame/index}]
   ["/assets/*" (ring/create-resource-handler)]])

(defrecord Router [db]
  component/Lifecycle
  (start [this]
         (assoc this :ring-router (ring/router routes))))

(defn make-router
  []
  (map->Router {}))
