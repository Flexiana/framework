(ns app.controllers.index
  (:require
    [xiana.core :as x]))

(defn index-view
  [state]
  (let [user (get-in state [:session-data :user])
        body (if user
               (format "Index page, for %s" (:first-name user))
               "Index page")]
    (x/ok
      (assoc state
             :response
             {:status  200
              :headers {"Content-Type" "text/plain"}
              :body    body}))))

(defn index
  [state]
  (x/flow->
    state
    index-view))
