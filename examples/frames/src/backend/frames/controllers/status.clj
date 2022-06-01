(ns frames.controllers.status
  (:require
    [ring.util.response :as response]))

(defn handle-status
  [state]
  (assoc state :response
         (response/response {:status "OK"})))
