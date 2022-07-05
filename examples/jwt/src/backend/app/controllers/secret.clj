(ns app.controllers.secret)

(defn protected-controller
  [state]
  (assoc state
         :response {:status  200
                    :headers {"Content-Type" "application/json"}
                    :body    (str "Hello " (get-in state [:session-data :first-name]) ". request content: " (get-in state [:request :body-params]))}))
