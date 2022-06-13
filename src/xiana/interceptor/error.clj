(ns xiana.interceptor.error)

(def response
  "Handles the exception if there's `ex-info` exception with non-empty
  `:xiana/response` key."
  {:name ::response
   :error (fn [state]
            (if-let [resp (-> state :error ex-data :xiana/response)]
              (-> state
                  (assoc :response resp)
                  (dissoc :error))
              state))})
