(ns middlewares.core
  (:require
    [view.core :as xview]
    [xiana.core :as xiana]))

(defn change-lang-middleware
  [state]
  ;; (clojure.pprint/pprint state)
  (-> state
      (assoc :lang :fr)
      xiana/ok))

(defn pre-route-middlewares
  [state]
  (xiana/flow-> state
                xiana/ok))

(defn pre-controller-middlewares
  [state]
  (xiana/flow-> state
                xiana/ok))

(defn post-controller-middlewares
  [state]
  (xiana/flow-> state
                (xview/view :generate-response)
                xiana/ok))
