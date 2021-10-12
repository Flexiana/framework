(ns framework.cookies.core
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.cookies :as cookies]
    [xiana.commons :refer [map-keys]]
    [xiana.core :as xiana]))

(defn parse-request-cookies
  [req]
  (keywordize-keys
    (cookies/cookies-request req)))

(defn parse-response-cookies
  [resp]
  (cookies/cookies-response resp))

(def stringify-keys (partial map-keys name))

(def interceptor
  "Extract parameters from request, should be middleware, or interceptor"
  {:enter (fn [state]
            (xiana/ok
              (update state :request
                      (comp parse-request-cookies #(update % :headers stringify-keys)))))
   :exit  (fn [state]
            (xiana/ok (update state :response parse-response-cookies)))})
