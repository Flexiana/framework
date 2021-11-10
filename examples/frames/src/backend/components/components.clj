(ns components
  (:require
    [controllers.index :as index]
    [controllers.status :as status]
    [framework.config.core :as config]
    [framework.interceptor.core :as interceptor]
    [framework.route.core :as router]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]))

(def routes
  [["/" {:get {:action index/handle-index}}]
   ["/status" {:get {:action status/handle-status}}]
   ["/assets/*" (ring/create-resource-handler {:path "/"})]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      router/reset
      ws/start
      (select-keys [:routes
                    :webserver
                    :controller-interceptors])
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [(interceptor/muuntaja)]})

(defn -main
  [& args]
  (->system app-cfg))
