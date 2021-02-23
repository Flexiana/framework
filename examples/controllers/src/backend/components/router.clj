(ns router
  (:require
    [com.stuartsierra.component :as component]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [controllers.rest :as rest]
    ;TODO do we want to require every part of domain logic here?
    [my-domain-logic.siege-machines :as mydomain.siege-machines]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [reitit.coercion.spec :as rcs]
    [clojure.spec.alpha :as s]))

(s/def :mydomain/id int?)
(s/def :mydomain/siege-machine keyword?)
(s/def :mydomain/villager keyword?)

;; TODO: refactor to smth like
;(route->
;  :homepage (GET "/" app.controllers.homepage/index-action)
;  :dynamic-content (dynamic "/:content-type/:url" (somefunction)))
;:static-content ["/assets/*" (ring/create-resource-handler)]
(def routes
  [["/" {:controller index/index}]
   ["/re-frame" {:controller re-frame/index}]
   ["" {:controller rest/ctrl
        :coercion rcs/coercion
        :middleware [rrc/coerce-request-middleware
                     rrc/coerce-response-middleware]} ;TODO handle error or catch an ExceptionInfo and process its data. At this moment catched at REST controller
    ["/api/siege-machines/:id" {:get mydomain.siege-machines/get-by-id
                                :parameters {:path {:id :mydomain/id}}
                                :responses {200 {:body :mydomain/siege-machine}}}]
    ["/api/villagers/{mydomain/id}/tasklist"]]
   ;["/api/*" {:controller rest/ctrl}] ;TODO we can have a separate router for API (per-model routes...)
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
