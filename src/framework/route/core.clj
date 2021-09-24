(ns framework.route.core
  (:require
    [framework.route.helpers :as helpers]
    [reitit.core :as r]
    [xiana.commons :refer [?assoc-in]]
    [xiana.core :as xiana]))

;; routes reference
(defonce -routes (atom []))


(defn reset
  "Update routes."
  [routes]
  (reset! -routes routes))


(defmacro -get-in-template
  "Simple macro to get the values from the match template."
  [t m k p]
  `(or (-> ~t :data ~p)
       (-> ~t ~k ~m ~p)))


(defn- -update
  "Update state with router match template data."
  [match {request :request :as state}]
  (let [method (:request-method request)
        handler (-get-in-template match method :result :handler)
        action (-get-in-template match method :data :action)
        interceptors (-get-in-template match method :data :interceptors)]
    ;; associate the necessary route match information
    (xiana/ok
      (-> state
          (?assoc-in [:request-data :method] method)
          (?assoc-in [:request-data :handler] handler)
          (?assoc-in [:request-data :interceptors] interceptors)
          (?assoc-in [:request-data :match] match)
          (assoc-in [:request-data :action]
            (or action
                (if handler
                  helpers/action
                  helpers/not-found)))))))


(defn match
  "Associate router match template data into the state.
  Return the wrapped state container."
  [{request :request :as state}]
  (let [match (r/match-by-path (r/router @-routes) (:uri request))]
    (-update match state)))
