(ns interceptors
  (:require
    [malli.core :as m]
    [reitit.coercion :as coercion]
    [reitit.core :as r]
    [xiana.core :as xiana]))

(def require-logged-in
  {:enter (fn [{request :request :as state}]
            (if-let [authorization (get-in request [:headers :authorization])]
              (xiana/ok (assoc-in state [:session-data :authorization] authorization))
              (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))})
