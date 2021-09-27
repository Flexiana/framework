(ns controllers.index
  (:require
    [xiana.core :as xiana]))

(defn index-view
  [state]
  (if (:current-user state)
    (prn "You are logged in"))
  (xiana/ok
    (assoc state
           :response
           {:status  200
            :headers {"Content-Type" "text/plain"}
            :body    "Index page"})))

(defn something-else
  [state]
  (xiana/ok state))

(defn moj
  [store]
  (prn "\n\n")

  (prn "here")
  (prn (-> store

           :current-user))

  (xiana/ok store))

(defn index
  [state]
  (xiana/flow->
    state
    (moj)

    (something-else)
    (index-view)))
