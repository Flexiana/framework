(ns framework.components.router.core
  (:require
    [com.stuartsierra.component :as component]
    [reitit.ring :as ring]))

(defrecord Router
  [db]
  component/Lifecycle
  (stop [this] this)
  (start
    [this]
    (assoc this :ring-router (ring/router (concat
                                            (:routes (:specific-component this)) ; adds routes specific to some component
                                            (:custom-routes this))))))

(defn make-router
  [routes]
  (map->Router {:custom-routes routes}))
