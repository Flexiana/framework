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

(def coerce
  {:enter (fn [{req :request :as state}]
            (let [cc (-> state
                         (get-in [:deps :routes])
                         (r/router {:compile coercion/compile-request-coercers})
                         (r/match-by-path (:uri req))
                         coercion/coerce!)]
              (xiana/ok (update-in state [:request :params] merge cc))))
   :error (fn [state]
            (xiana/error (assoc state :response {:status 400
                                                 :body   "Request coercion failed"})))
   :leave (fn [{{:keys [:status :body]} :response
                :as                     state}]
            (let [schema (get-in state [:request-data :match :data :responses status :body])]
              (cond (and schema body (m/validate schema body)) (xiana/ok state)
                    (and schema body) (xiana/error (assoc state :response {:status 400
                                                                           :body   "Response validation failed"}))
                    :else (xiana/ok state))))})
