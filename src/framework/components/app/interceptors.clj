(ns framework.components.app.interceptors
  (:require
    [xiana.commons :refer :all]
    [xiana.core :as xiana]))

(comment
  (def sample-router-interceptor
    {:enter (fn [{request :request :as state}]
              (xiana/ok state))
     :leave (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
              (xiana/ok state))})

  (def sample-controller-interceptor
    {:enter (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
              (xiana/ok state))
     :leave (fn [{request :request response :response :as state}]
              (xiana/ok state))}))

(def wrap-path-params
  {:leave (fn [{{:keys [match]} :request-data :as state}]
            (xiana/ok (-> state (?assoc-in [:request :path-params] (:path-params match)))))})
