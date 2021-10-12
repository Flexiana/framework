(ns framework.coercion.core
  (:require [reitit.core :as r]
            [reitit.coercion :as coercion]
            [xiana.core :as xiana]
            [malli.core :as m]))

(def interceptor
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