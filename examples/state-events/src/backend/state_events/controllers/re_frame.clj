(ns state-events.controllers.re-frame
  (:require [xiana.core :as xiana]
            [ring.util.response :as ring]))

(defn handle-index
  [state]
  (xiana/ok
    (assoc state
      :response
      (-> "index.html"
          (ring/resource-response {:root "public"})
          (ring/header "Content-Type" "text/html; charset=utf-8")))))
