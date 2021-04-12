(ns router
  (:require
    [com.stuartsierra.component :as component]
    [controllers.index :as index]
    [controllers.login :as login]
    [controllers.logout :as logout]
    [controllers.re-frame :as re-frame]
    [controllers.secret :as secret]
    [reitit.ring :as ring]))

;; TODO: refactor to smth like
;(route->
;  :homepage (GET "/" app.controllers.homepage/index-action)
;  :dynamic-content (dynamic "/:content-type/:url" (somefunction)))
;:static-content ["/assets/*" (ring/create-resource-handler)]



(def routes
  [["/" {:action index/index}]
   ["/login" {:action login/login-controller}]
   ["/logout" {:action logout/logout-controller}]
   ["/secret" {:action secret/protected-controller}]
   ["/re-frame" {:action re-frame/index}]
   ["/assets/*" (ring/create-resource-handler)]])

(defrecord Router
  [db]
  component/Lifecycle
  (stop [this] this)
  (start
    [this]
    (assoc this :ring-router (ring/router routes))))

(defn make-router
  []
  (map->Router {}))
