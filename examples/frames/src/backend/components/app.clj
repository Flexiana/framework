(ns app
  (:require
    [controllers.index :as index]
    [controllers.status :as status]
    [framework.webserver.core :as ws]
    [reitit.ring :as ring]))

(def routes
  ["" {:handler ws/handler-fn}
   ["/" {:get {:action index/handle-index}}]
   ["/status" {:get {:action status/handle-status}}]
   ["/assets/*" {:get {:action (ring/create-resource-handler)}}]])

