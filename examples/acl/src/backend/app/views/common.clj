(ns views.common)

(defn response
  [state body]
  (->
    state
    (assoc-in [:response :status] 200)
    (assoc-in [:response :headers "Content-type"] "Application/json")
    (assoc-in [:response :body] body)))

(defn not-allowed
  [state]
  (assoc state :response {:status 401 :body "You don't have rights to do this"}))
