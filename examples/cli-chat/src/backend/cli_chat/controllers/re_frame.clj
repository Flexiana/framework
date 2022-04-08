(ns cli-chat.controllers.re-frame
  (:require
    [ring.util.response :as ring]))

(defn handle-index
  [state]
  (assoc state
         :response
         (-> "index.html"
             (ring/resource-response {:root "public"})
             (ring/header "Content-Type" "text/html; charset=utf-8"))))
