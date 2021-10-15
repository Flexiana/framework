(ns app.controllers.logout
  (:require
    [xiana.core :as x]))

(defn logout-view
  [state]
  (let [user (get-in state [:session-data :user])]
    (x/ok (assoc state
                 :response {:status  200
                            :headers {"Content-Type" "application/json"}
                            :body    (str (:first-name user) " logged out")}))))

(defn logout-controller
  [state]
  (x/flow-> state
            logout-view))
