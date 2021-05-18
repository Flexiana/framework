(ns framework.route.core
  (:require
   [reitit.core :as r]
   [xiana.core :as xiana]
   [xiana.commons :refer [?assoc-in]]
   [framework.route.helpers :as helpers]))

;; routes reference
(defonce -routes (atom []))

(defn reset
  "Update routes."
  [routes]
  (reset! -routes routes))

(defn- -get-action
  "Fetch action from the match template map."
  [match method]
  (or (-> match :data :action)
      (-> match :data method :action)))

(defn -get-interceptors
  "Fetch interceptors from the match, route or controllers ones."
  [match method]
  (or (-> match :data :interceptors)
      (-> match :data method :interceptors)))

(defn- -get-handler
  "Get handler from the match template map."
  [match method]
  (or (-> match :data :handler)
      (-> match :result method :handler)))

(defn- -update
  "Update state with router match template data."
  [match {request :request :as state}]
  (let [method (:request-method request)
        handler (-get-handler match method)
        action  (-get-action match method)
        interceptors (-get-interceptors match method)]
    ;; associate the necessary route match information
    (xiana/ok
     (-> state
         (?assoc-in [:request-data :method] method)
         (?assoc-in [:request-data :handler] handler)
         (?assoc-in [:request-data :interceptors] interceptors)
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
