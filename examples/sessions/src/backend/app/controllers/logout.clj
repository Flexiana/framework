(ns app.controllers.logout
  (:require
    [xiana.session :refer [delete!]]))

(defn logout-controller
  [state]
  (let [user (get-in state [:session-data :user])
        sessions-backend (-> state
                             :deps
                             :session-backend)
        session-id (-> state
                       :session-data
                       :session-id)]
    (delete! sessions-backend session-id)
    (assoc (dissoc state :session-data)
           :response {:status  200
                      :headers {"Content-Type" "application/json"}
                      :body    (str (:first-name user) " logged out")})))
