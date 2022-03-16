(ns app.core
  (:require
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.logout :as logout]
    [app.controllers.secret :as secret]
    [app.interceptors :refer [inject-session?
                              logout
                              require-logged-in]]
    [framework.config.core :as config]
    [framework.handler.core :as x-handler]
    [framework.interceptor.core :as x-interceptors]
    [framework.interceptor.error]
    [framework.interceptor.muuntaja]
    [framework.route.core :as x-routes]
    [framework.session.core :as x-session]
    [framework.webserver.core :as ws]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]))

(def routes
  [["" {:handler x-handler/handler-fn}]
   ["/" {:action       index/index
         :interceptors [x-interceptors/params
                        inject-session?]}]
   ["/login" {:action login/login-controller}]
   ["/logout" {:action       logout/logout-controller
               :interceptors {:around [logout]}}]
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
  {:routes routes
   :controller-interceptors [;; x-interceptors/params
                             framework.interceptor.muuntaja/interceptor
                             framework.interceptor.error/handle-ex-info
                             ;; (x-session/protected-interceptor "" "/login")
                             ]})

(defn -main
  [& _args]
  (->system app-cfg))
