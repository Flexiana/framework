(ns app.router
  (:require
    [app.controllers.index :as index]
    [app.controllers.login :as login]
    [app.controllers.logout :as logout]
    [app.controllers.secret :as secret]
    [app.interceptors :refer [inject-session?
                              logout
                              require-logged-in]]
    [framework.interceptor.core :as x-interceptors]
    [framework.webserver.core :as x-ws]))

(def routes
  [["" {:handler x-ws/handler-fn}]
   ["/" {:action       index/index
         :interceptors [x-interceptors/params
                        inject-session?]}]
   ["/login" {:action login/login-controller}]
   ["/logout" {:action       logout/logout-controller
               :interceptors {:around [logout]}}]
   ["/secret" {:action       secret/protected-controller
               :interceptors {:inside [require-logged-in]}}]])