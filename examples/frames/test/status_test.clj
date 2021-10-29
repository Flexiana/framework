(ns status-test
  (:require
    [clojure.test :refer :all]
    [components]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]))

(deftest status-test
  (let [deps (components/deps (config/env))
        request {:uri            "/status"
                 :request-method :get}
        handle (handler-fn deps)
        visit (-> (handle request)
                  (update :body slurp))]
    (is (= 200 (:status visit)))
    (is (= "{\"status\":\"OK\"}" (:body visit)))))
