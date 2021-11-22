(ns cli-chat.controllers.re-frame
  (:require
    [ring.util.response :as ring]
    [xiana.core :as xiana]))

(defn handle-index
  [state]
  (xiana/ok
    (assoc state
           :response
           (-> "index.html"
               (ring/resource-response {:root "public"})
               (ring/header "Content-Type" "text/html; charset=utf-8")))))
