(ns controllers.logout
  (:require
    [xiana.core :as xiana]))

(defn logout-view
  [state]
  (let [session-id (get-in state [:session-data :session-id])
        user (get-in state [:session-data :user])]
    (xiana/ok (assoc state
                     :session-data {}
                     :response {:status  200
                                :headers {"Content-Type" "application/json"}
                                :body    (str (:first-name user) " logged out")}
                     :logout-data {:session-id session-id}))))

(defn logout-controller
  [state]
  (xiana/flow-> state
                logout-view))
