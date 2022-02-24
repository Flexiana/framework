(ns interceptors
  (:require
    [xiana.core :as xiana]))

(def require-logged-in
  {:enter (fn [{request :request :as state}]
            (if-let [authorization (get-in request [:headers :authorization])]
              (xiana/ok (assoc-in state [:session-data :authorization] authorization))
              (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))})
