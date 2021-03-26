(ns sessions.auth
  (:require [xiana.core :as xiana]))

(defn require-logged-in
  [state]
  (if (get-in state [:session-data :user])
    (xiana/ok state)
    (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))
