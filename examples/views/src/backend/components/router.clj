(ns router
  (:require
    [com.stuartsierra.component :as component]
    [controllers.home-ctrl :as hctrl]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [controllers.records-ctrl :as rctrl]
    [reitit.ring :as ring]
    [view.core :as xviews]
    [xiana.core :as xiana]))

(def routes
  [["/"
    [""
     ["{language}" {:controller hctrl/home-controller}]]]
   ["/:language/records" {:controller rctrl/records-controller}]
   ["/:language/re-frame" {:controller re-frame/index}]
   ["/:language/assets/*" (ring/create-resource-handler)]])

(defrecord Router [db]
  component/Lifecycle
  (start [this]
         (assoc this :ring-router (ring/router routes)))
  (stop [this]
        (assoc this :ring-router nil)))

(defn make-router
  []
  (map->Router {}))
