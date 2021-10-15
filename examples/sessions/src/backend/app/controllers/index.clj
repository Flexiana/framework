(ns controllers.index
  (:require
    [xiana.core :as xiana]))

(defn index-view
  [state]
  (xiana/ok
    (assoc state
           :response
           {:status  200
            :headers {"Content-Type" "text/plain"}
            :body    "Index page"})))

(defn index
  [state]
  (xiana/flow->
    state
    index-view))
