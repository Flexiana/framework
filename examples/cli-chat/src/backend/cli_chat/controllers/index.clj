(ns cli-chat.controllers.index
  (:require [xiana.core :as xiana]
            [ring.util.response :as ring]))

(defn handle-index
  [state]
  (xiana/ok
    (assoc state
      :response
      (ring/response "Index page"))))
