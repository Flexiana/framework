(ns xiana.route.helpers
  "The default not found, unauthorized and action functions")

(defn not-found
  "Default not-found response handler helper."
  [state]
  (assoc state :response {:status 404 :body "Not Found"}))

(defn unauthorized
  "Default unauthorized response handler helper."
  [state msg]
  (assoc state :response {:status 401 :body msg}))

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
