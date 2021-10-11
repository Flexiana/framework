(ns interceptors
  (:require
    [malli.core :as m]
    [potemkin :refer [import-vars]]
    [reitit.coercion :as coercion]
    [reitit.core :as r]
    [router]
    [xiana.core :as xiana]))

(import-vars
  [framework.interceptor.core
   params])

(def require-logged-in
  {:enter (fn [{request :request :as state}]
            (if-let [authorization (get-in request [:headers :authorization])]
              (xiana/ok (assoc-in state [:session-data :authorization] authorization))
              (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))})

(def coerce
  {:enter (fn [{req :request :as state}]
            (let [uri (:uri req)
                  routes (get-in state [:deps :routes])
                  match (r/match-by-path (r/router routes {:compile coercion/compile-request-coercers}) uri)
                  coercion (coercion/coerce! match)]
              (xiana/ok (update-in state [:request :params] merge coercion))))
   :error (fn [state]
            (xiana/error (assoc state :response {:status 400
                                                 :body   "Request coercion failed"})))
   :leave (fn [{response :response :as st}]
            (let [responses (get-in st [:request-data :match :data :responses])
                  {:keys [status body]} (select-keys response [:status :body])
                  schema (get-in responses [status :body])
                  valid (if (and schema body)
                          (m/validate schema body)
                          true)]
              (if valid
                (xiana/ok st)
                (xiana/error (assoc st :response {:status 400
                                                  :body "Response validation failed"})))))})
