(ns xiana.route.helpers
  "The default not found and action functions")

(defn not-found
  "Default not-found response handler helper."
  [state]
  (-> state
      (assoc :response {:status 404 :body "Not Found"})))

(defn action
  "Default action response handler helper."
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (assoc state :response (handler request))
    (catch Exception e
      (-> state
          (assoc :error e)
          (assoc :response
                 {:status 500 :body "Internal Server error"})))))
