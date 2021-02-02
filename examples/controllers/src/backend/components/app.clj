(ns app
  (:require [reitit.core :as r]
            [com.stuartsierra.component :as component]))

(defn- create-ring-handler
  [ring-config router db]
  (fn [request]
    (let [match (r/match-by-path (:ring-router router) (:uri request))
          action (get-in match [:data :action])]
      (if action
        (action request {} {})
        {:status 404
         :body   "Not Found"}))))

(defrecord App [config router db]
  component/Lifecycle
  (start [this]
    (assoc this :handler (create-ring-handler config router db))))

(defn make-app
  [config]
  (map->App {:config config}))