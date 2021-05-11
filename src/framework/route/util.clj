(ns framework.route.util
  (:require
   [xiana.core :as xiana]))

(defn not-found
  "Default not-found response handler."
  [state]
  (xiana/error
   (-> state
       (assoc :response {:status 404 :body "Not Found"}))))

;; TODO: research!
(defn action
  "Default action response handler."
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (xiana/ok
     (assoc state :response (handler request)))
    (catch Exception e
      (xiana/error
       (-> state
           (assoc :erorr e)
           (assoc :response
                  {:status 500 :body "Internal Server error"}))))))
