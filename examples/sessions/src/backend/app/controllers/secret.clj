(ns controllers.secret
  (:require
    [sessions.auth :as auth]
    [xiana.core :as xiana]))

(defn protected-view
  [{request :http-request :as state}]
  (xiana/ok (assoc state
              :response {:status 200
                         :headers {"Content-Type" "application/json"}
                         :body (str "Hello " (get-in request [:user :first-name]))})))

(defn protected-controller
  [state]
  (xiana/flow-> state
                auth/require-logged-in
                protected-view))
