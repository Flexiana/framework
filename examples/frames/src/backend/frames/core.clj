(ns frames.core
  (:require
    [frames.controllers.index :as index]
    [frames.controllers.status :as status]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [reitit.ring :as ring]
    [xiana.config :as config]
    [xiana.interceptor :as interceptor]
    [xiana.route :as router]
    [xiana.webserver :as ws]))

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
