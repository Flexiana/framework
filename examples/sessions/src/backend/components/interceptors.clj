(ns interceptors
  (:require
    [framework.session.core :refer [add! delete!]]
    [xiana.core :as xiana]))

(def require-logged-in
  {:enter
   (fn [state]
     (if (get-in state [:session-data :user])
       (xiana/ok state)
       (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))})

(def login-out
  {:leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  login-data (-> state
                                 :login-data)
                  logout-data (-> state
                                  :logout-data)
                  session-id (get login-data :session-id (:session-id logout-data))]
              (when login-data
                (add! sessions-backend session-id login-data))
              (when logout-data
                (delete! sessions-backend session-id))
              (xiana/ok (assoc-in state [:session-data] (or login-data logout-data)))))})
