(ns xiana.interceptor.error
  (:require [ring.util.response :as ring]))

(def response
  "Handles the exception if there's `ex-info` exception with non-empty
  `:xiana/response` key."
  {:name  ::error-response
   :error (fn [state]
            (if-let [resp (-> state :error ex-data :xiana/response)]
              (-> state
                  (assoc :response (ring/bad-request resp))
                  (dissoc :error))
              state))})
