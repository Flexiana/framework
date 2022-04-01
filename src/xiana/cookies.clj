(ns xiana.cookies
  "Cookie parser"
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.cookies :as cookies]
    [xiana.core :as xiana]))

(def interceptor
  "Parses request and response cookies"
  (letfn [(move-cookies [{headers :headers :as req}]
            (cond-> req
              (not (get headers "cookie")) (assoc-in
                                             [:headers "cookie"]
                                             (:cookie headers))))
          (parse-request-cookies [req]
            (-> req move-cookies cookies/cookies-request keywordize-keys))]
    {:enter (fn [state]
              (xiana/ok
                (update state :request
                        parse-request-cookies)))
     :leave (fn [state]
              (xiana/ok (update state :response cookies/cookies-response)))}))
