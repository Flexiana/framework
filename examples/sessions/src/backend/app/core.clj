(ns app.core
  (:require
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.logout :as logout]
    [app.controllers.secret :as secret]
    [app.interceptors :refer [inject-session?
                              require-logged-in]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [xiana.config :as config]
    [xiana.handler :as x-handler]
    [xiana.interceptor :as x-interceptors]
    [xiana.route :as x-routes]
    [xiana.session :as x-session]
    [xiana.webserver :as ws]))

(def routes
  [["" {:handler x-handler/handler-fn}]
   ["/" {:action       index/index
         :interceptors [x-interceptors/params
                        inject-session?]}]
   ["/login" {:action login/login-controller
              :interceptors {:except [xiana.session/interceptor]}}]
   ["/logout" {:action       logout/logout-controller}]
   ["/secret" {:action       secret/protected-controller
               :interceptors {:inside [require-logged-in]}}]])

(defn ->system
  [app-cfg]
  (-> (config/config)
      (merge app-cfg)
      x-routes/reset
      x-session/init-backend
      ws/start
      closeable-map))

(def app-cfg
  {:routes                  routes
   :controller-interceptors [x-interceptors/params
                             xiana.interceptor.muuntaja/interceptor
                             xiana.interceptor/handle-ex-info
                             xiana.session/interceptor]})

(defn -main
  [& _args]
  (->system app-cfg))
