(ns middlewares.core
  (:require
    [view.core :as xview]
    [xiana.core :as xiana]))

(defn change-lang-middleware
  [state]
  (-> state
      (assoc-in [:lang] :fr)
      xiana/ok))

(defn pre-route-middlewares
  [state]
  (xiana/flow-> state
                ;; (xview/set-lang-by-query-params)
                xiana/ok))

(defn pre-controller-middlewares
  [state]
  (xiana/flow-> state
                xiana/ok))

(defn post-controller-middlewares
  [state]
  (xiana/flow-> state
                ;; (change-lang-middleware)
                (xview/generate-response)
                xiana/ok))
