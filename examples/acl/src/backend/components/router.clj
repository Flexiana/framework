(ns router
  (:require
    [com.stuartsierra.component :as component]
    [controllers.index :as index]
    [controllers.posts :as posts]
    [controllers.re-frame :as re-frame]
    [reitit.ring :as ring]))

;; TODO: refactor to smth like
;(route->
;  :homepage (GET "/" app.controllers.homepage/index-action)
;  :dynamic-content (dynamic "/:content-type/:url" (somefunction)))
;:static-content ["/assets/*" (ring/create-resource-handler)]
(def routes
  [["/" {:controller index/index}]
   ["/re-frame" {:controller re-frame/index}]
   ["/posts" {:controller posts/controller}]

   ["/assets/*" (ring/create-resource-handler)]])

(defrecord Router [db]
  component/Lifecycle
  (start [this]
         (assoc this :ring-router (ring/router routes)))
  (stop [this]))

(defn make-router
  []
  (map->Router {}))
