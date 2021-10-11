(ns todoapp.views.common
  (:require
    [xiana.core :as xiana]))

(defn response
  [state body]
  (->
    state
    (assoc-in [:response :status] 200)
    (assoc-in [:response :headers "content-type"] "application/json")
    (assoc-in [:response :body] body)))

(defn not-allowed
  [state]
  (xiana/error
   (assoc state
          :response {:status 403
                     :body "You don't have rights to do this"})))
