(ns framework.cookies.core
  "Cookie parser"
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.cookies :as cookies]
    [xiana.commons :refer [map-keys]]
    [xiana.core :as xiana]))

(defn- parse-request-cookies
  [req]
  (keywordize-keys
    (cookies/cookies-request req)))

(defn- parse-response-cookies
  [resp]
  (cookies/cookies-response resp))

(def interceptor
  "Parses request and response cookies"
  {:enter (fn [state]
            (xiana/ok
              (update state :request
                      parse-request-cookies)))
   :exit  (fn [state]
            (xiana/ok (update state :response parse-response-cookies)))})
