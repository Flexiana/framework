(ns core
  (:require
    [interceptors :as app-interceptors]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [route]
    [xiana.coercion :as coercion]
    [xiana.config :as config]
    [xiana.interceptor :as interceptors]
    [xiana.route :as routes]
    [xiana.webserver :as ws]))

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      routes/reset
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  route/routes
   :controller-interceptors [(interceptors/muuntaja)
                             interceptors/params
                             coercion/interceptor
                             app-interceptors/require-logged-in]
   :error-interceptors [(interceptors/muuntaja)]})

(defn -main
  [& _args]
  (->system app-cfg))
