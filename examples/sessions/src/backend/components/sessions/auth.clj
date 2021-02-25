(ns sessions.auth
  (:require [xiana.core :as xiana]))



(defn require-logged-in
  [{req :http-request :as state}]
  (if (get req :user)
    (xiana/ok state)
    (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))

