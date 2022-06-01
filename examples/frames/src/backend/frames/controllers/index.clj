(ns frames.controllers.index
  (:require
    [ring.util.response :as response]))

(defn handle-index
  [state]
  (assoc state :response
         (-> "index.html"
             (response/resource-response {:root "public"})
             (response/header "Content-Type" "text/html; charset=utf-8"))))
