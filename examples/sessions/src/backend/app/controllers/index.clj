(ns controllers.index
  (:require
    [xiana.core :as xiana]))

(defn index-view
  [state]
  (let [user (get-in state [:session-data :user])
        body (if user
               (format "Index page, for %s" (:first-name user))
               "Index page")]
    (xiana/ok
      (assoc state
             :response
             {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    body}))))

(defn index
  [state]
  (xiana/flow->
    state
    index-view))
