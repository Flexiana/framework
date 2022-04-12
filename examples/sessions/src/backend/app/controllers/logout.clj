(ns app.controllers.logout)

(defn logout-controller
  [state]
  (let [user (get-in state [:session-data :user])]
    (assoc state
           :response {:status  200
                      :headers {"Content-Type" "application/json"}}
           :body    (str (:first-name user) " logged out"))))
