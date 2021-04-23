(ns framework.test-interceptor
  (:require
    [xiana.core :as xiana]))

(def test-interceptor
  {:enter (fn [state] (xiana/ok (assoc-in state [:response :enter] true)))
   :leave (fn [state] (xiana/ok (assoc-in state [:response :leave] true)))})
