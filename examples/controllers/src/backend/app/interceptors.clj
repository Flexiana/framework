(ns interceptors
  (:require
    [potemkin :refer [import-vars]]
    [xiana.core :as xiana]))


(import-vars
  [framework.components.app.interceptors
   wrap-path-params])

(def require-logged-in
  {:enter (fn [{request :request {:keys [handler action match]} :request-data :as state}]
              (if-let [authorization (get-in request [:headers "authorization"])]
                        (xiana/ok (assoc-in state [:session-data :authorization] authorization))
                        (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))})
