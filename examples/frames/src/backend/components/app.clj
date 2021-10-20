(ns app
  (:require
    [controllers.index :as index]
    [controllers.status :as status]
    [framework.handler.core :refer [handler-fn]]
    [reitit.ring :as ring]))

(def routes
  ["" {:handler handler-fn}
   ["/" {:get {:action index/handle-index}}]
   ["/status" {:get {:action status/handle-status}}]
   ["/assets/*" (ring/create-resource-handler {:path "/"})]])

