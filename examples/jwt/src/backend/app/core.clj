(ns app.core
  (:require
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.secret :as secret]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.config :as config]
    [xiana.handler :as x-handler]
    [xiana.interceptor :as x-interceptors]
    [xiana.interceptor.error]
    [xiana.route :as x-routes]
    [xiana.webserver :as ws]))

(def routes
  [["" {:handler x-handler/handler-fn}]
   ["/" {:action       index/index
         :interceptors [x-interceptors/params]}]
   ["/login" {:action login/login-controller
              :interceptors {:except [x-interceptors/]}}]
   ["/secret" {:action       secret/protected-controller}]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      x-routes/reset
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [(x-interceptors/muuntaja)
                             (x-interceptors/jwt-auth)
                             (x-interceptors/jwt-content)
                             xiana.interceptor.error/response
                             x-interceptors/params]})

(defn -main
  [& _args]
  (->system app-cfg))
