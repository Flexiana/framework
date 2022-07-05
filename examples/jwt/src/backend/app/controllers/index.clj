(ns app.controllers.index)

(defn index
  [state]
  (let [user (:session-data state)
        body (if user
               (format "Index page, for %s" (:first-name user))
               "Index page")]
    (assoc state
           :response
           {:status  200
            :headers {"Content-Type" "text/plain"}
            :body    body})))
