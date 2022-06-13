(ns state-events.interceptors
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.cookies :as cookies]))

(def asset-router
  {:enter (fn [{{uri :uri} :request
                :as        state}]
            (case uri
              "/favicon.ico" (assoc-in state [:request :uri] "/assets/favicon.ico")
              state))})

(def session-id->cookie
  {:leave (fn [state]
            (-> (assoc-in state
                          [:response :cookies :session-id]
                          (get-in state [:session-data :session-id]))
                (assoc-in
                  [:response :headers "access-control-expose-headers"]
                  "Set-Cookie")))})
