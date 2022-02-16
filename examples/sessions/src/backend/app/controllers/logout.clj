(ns app.controllers.logout
  (:require
    [xiana.core :as xiana]))

(defn logout-view
  [state]
  (let [user (get-in state [:session-data :user])]
    (xiana/ok (assoc state
                 :response {:status  200
                            :headers {"Content-Type" "application/json"}
                            :body    (str (:first-name user) " logged out")}))))

(defn logout-controller
  [state]
  (xiana/flow-> state
            logout-view))
