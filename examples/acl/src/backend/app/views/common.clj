(ns views.common
  (:require
    [xiana.core :as xiana]))

(defn response
  [state body]
  (->
    state
    (assoc-in [:response :status] 200)
    (assoc-in [:response :headers "Content-type"] "Application/json")
    (assoc-in [:response :body] body)))

(defn not-allowed
  [state]
  (xiana/error (assoc state :response {:status 401 :body "You don't have rights to do this"})))
