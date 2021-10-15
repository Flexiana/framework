(ns controllers.secret
  (:require
    [xiana.core :as xiana]))

(defn protected-view
  [state]
  (xiana/ok (assoc state
                   :response {:status  200
                              :headers {"Content-Type" "application/json"}
                              :body    (str "Hello " (get-in state [:session-data :user :first-name]))})))

(defn protected-controller
  [state]
  (xiana/flow-> state
                protected-view))
