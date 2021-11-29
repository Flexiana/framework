(ns framework.cookies.core
  "Cookie parser"
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.cookies :as cookies]
    [xiana.core :as xiana]))

(def interceptor
  "Parses request and response cookies"
  (letfn [(move-cookies
            [req]
            (if (get-in req [:headers "cookie"])
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
