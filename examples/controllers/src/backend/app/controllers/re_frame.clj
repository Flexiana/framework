(ns controllers.re-frame
  (:require [xiana.core :as xiana]
            [ring.util.response :as response]))

(defn index
  [state]
  (xiana/ok
    (assoc state
      :response
      (-> "index.html"
          (response/resource-response {:root "public"})
          (response/header "Content-Type" "text/html; charset=utf-8")))))
