(ns state-events.interceptors
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.cookies :as cookies]
    [xiana.core :as xiana]))

(def asset-router
  {:enter (fn [{{uri :uri} :request
                :as        state}]
            (xiana/ok
              (case uri
                "/favicon.ico" (assoc-in state [:request :uri] "/assets/favicon.ico")
                state)))})

(def session-id->cookie
  {:leave (fn [state]
            (xiana/ok
              (-> (assoc-in state
                            [:response :cookies :session-id]
                            (get-in state [:session-data :session-id]))
                  (assoc-in
                    [:response :headers "access-control-expose-headers"]
                    "Set-Cookie"))))})

(def cookies
  "Parses request and response cookies"
  (letfn [(move-cookies
            [req]
            (if (-> req :headers "cookie")
              req
              (assoc-in req [:headers "cookie"]
                        (get-in req [:headers :cookie]))))
          (parse-request-cookies
            [req]
            (keywordize-keys
              (cookies/cookies-request (move-cookies req))))
          (parse-response-cookies
            [resp]
            (cookies/cookies-response resp))]
    {:enter (fn [state]
              (xiana/ok
                (update state :request
                        parse-request-cookies)))
     :leave (fn [state]
              (xiana/ok (update state :response parse-response-cookies)))}))
