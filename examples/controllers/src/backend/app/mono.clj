(ns mono
  (:require [reitit.ring :as ring]))

(defn handler [request]
  (smth router request))

(def router
  (ring/router
    ["/ping" {:get handler}]))

(def app (ring/ring-handler router (constantly {:status 404, :body ""})))
