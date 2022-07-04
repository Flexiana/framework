(ns app.interceptors
  (:require
    [xiana.session :refer [delete! fetch]])
  (:import
    (java.util
      UUID)))

(def require-logged-in
  {:name ::require-logged-in
   :enter
   (fn [state]
     (if (get-in state [:session-data :user])
       state
       (throw (ex-info "Unauthorized" {:xiana/response {:status 401 :body "Unauthorized"}}))))})

(def logout
  {:name ::logout
   :leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id       (-> state
                                       :session-data
                                       :session-id)]
              (delete! sessions-backend session-id)
              (dissoc state :session-data)))})
