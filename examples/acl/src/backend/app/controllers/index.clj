(ns controllers.index
  (:require
    [ring.util.response :as ring]))

(defn handle-index
  [state]
  (assoc state
         :response
         (ring/response "Index page")))
