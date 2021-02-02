(ns router
  (:require [controllers.status :as status]
            [controllers.index :as index]
            [controllers.actions :as actions]
            [reitit.ring :as ring]
            [com.stuartsierra.component :as component]))

;; TODO: refactor to
; :NAME :PATTERN :ACTION
; :homepage [:GET "/"] app.controllers.index/index-action
(def routes
  [["/" {:action actions/index-action}]
   ])

(defrecord Router [db]
  component/Lifecycle
  (start [this]
    (assoc this :ring-router (ring/router routes))))

(defn make-router
  []
  (map->Router {}))