(ns state-events.interceptors
  (:require
    [xiana.core :as xiana]))

(def sample-state-events-controller-interceptor
  {:enter (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
            (xiana/ok state))
   :leave (fn [{response :response :as state}]
            (xiana/ok state))})
