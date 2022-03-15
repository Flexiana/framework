(ns frames.controllers.index
  (:require
    [ring.util.response :as response]
    [xiana.core :as xiana]))

(defn handle-index
  [state]
  (xiana/ok (assoc state :response
                   (-> "index.html"
                       (response/resource-response {:root "public"})
                       (response/header "Content-Type" "text/html; charset=utf-8")))))
