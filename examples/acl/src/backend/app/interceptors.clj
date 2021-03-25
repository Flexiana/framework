(ns interceptors
  (:require
    [potemkin :refer [import-vars]]
    [xiana.core :as xiana]))

(comment
  (import-vars
    [framework.components.app.interceptors
     sample-router-interceptor
     sample-controller-interceptor]))

(def sample-acl-controller-interceptor
  {:enter (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
            (xiana/ok state))})
