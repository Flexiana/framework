(ns conway.users.actions
  (:require
    [clj-http.client :as client]
    [muuntaja.core :as m]))


(def store (atom {}))


(defn greet
  [_]
  {:status 200
   :body "It works!"})


(defn all
  [_]
  {:status 200
   :body @store})


(defn create
  [{user :body-params}]
  (let [id (str (java.util.UUID/randomUUID))
        data (->> (assoc user :id id)
                  (swap! store assoc id))]
    {:status 200
     :body data}))


(defn supply
  [_]
  (let [answer (client/get "https://jsonplaceholder.typicode.com/users")
        data (swap! store assoc :data (->> answer :body (m/decode "application/json")))]
    {:status 200
     :body data}))
