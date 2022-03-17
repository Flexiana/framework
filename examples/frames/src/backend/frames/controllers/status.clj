(ns frames.controllers.status
  (:require
    [ring.util.response :as response]
    [xiana.core :as xiana]))

(defn handle-status
  [state]
  (xiana/ok
    (assoc state :response
           (response/response {:status "OK"}))))
