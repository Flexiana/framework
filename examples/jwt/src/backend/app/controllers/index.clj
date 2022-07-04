(ns app.controllers.index)

(defn index
  [state]
  (let [user (get-in state [:session-data :user])
        body (if user
               (format "Index page, for %s" (:first-name user))
               "Index page")]
    (assoc state
           :response
           {:status  200
            :headers {"Content-Type" "text/plain"}
            :body    body})))
