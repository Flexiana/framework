(ns controllers.logout
  (:require
    [clojure.data.json :as json]
    [ring.util.request :refer [body-string]]
    [xiana.core :as xiana]))

(defn logout-view
  [{request :http-request :as state}]

  (let [session-id (-> request
                       :headers
                       (get "session-id"))]
    (xiana/ok (assoc state
                     :response {:status 200
                                :headers {"Content-Type" "application/json"}
                                :body "foso"}
                     :logout-data {:session-id session-id}))))

(defn logout-controller
  [state]

  (xiana/flow-> state
                logout-view))
