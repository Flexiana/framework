(ns migrator.interceptors
  (:require
    [xiana.core :as xiana]))

(def sample-migrator-controller-interceptor
  {:enter (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
            (xiana/ok state))
   :leave (fn [{response :response :as state}]
            (xiana/ok state))})
