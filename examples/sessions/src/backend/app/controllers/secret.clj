(ns app.controllers.secret
  (:require
    [xiana.core :as x]))

(defn protected-view
  [state]
  (x/ok (assoc state
               :response {:status  200
                          :headers {"Content-Type" "application/json"}
                          :body    (str "Hello " (get-in state [:session-data :user :first-name]))})))

(defn protected-controller
  [state]
  (x/flow-> state
            protected-view))
