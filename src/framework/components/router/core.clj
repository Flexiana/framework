(ns framework.components.router.core
  (:require
    [com.stuartsierra.component :as component]
    [reitit.ring :as ring]))

(defrecord Router
  [db]
  component/Lifecycle
  (stop [this] this)
  (start [this]
         (assoc this :ring-router (ring/router (concat
                                                 (:routes (:specific-component this)) ; adds routes specific to some component
                                                 (:custom-routes this))))))

(defn make-router
  "DEPRECATED"
  [routes]
  (map->Router {:custom-routes routes}))

(defn ->router
  [_config routes]
  (with-meta {:custom-routes routes}
    `{component/start ~(fn [this]
                         (assoc this :router
                           (ring/router (concat
                                          (-> this :specific-component :routes)
                                          (-> this :custom-routes)))))
      component/stop  ~(fn [this]
                         (dissoc this :router))}))
