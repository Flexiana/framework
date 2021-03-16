(ns sessions.middleware
  (:require
    [sessions.backend :refer [fetch add! delete!]]
    [xiana.core :as xiana]))

(defn session-middleware
  [{request :http-request :as state}]
  (let [sessions-backend (-> state
                             :deps
                             :session
                             :session-backend)
        session-id (get-in request [:headers "session-id"])
        user  (fetch sessions-backend session-id)]
    (xiana/ok (assoc-in state [:deps :session :session-data :user] user))))

(defn auth-middleware
  [state]
  (let [sessions-backend (-> state
                             :deps
                             :session
                             :session-backend)
        login-data (-> state
                       :login-data)
        logout-data (-> state
                        :logout-data)]
    (when login-data
      (add! sessions-backend (:session-id login-data) login-data))
    (when logout-data
      (delete! sessions-backend (:session-id logout-data)))
    (xiana/ok state)))
