(ns status-test
  (:require
    [clojure.test :refer :all]
    [frames.core :as frames]
    [framework.handler.core :refer [handler-fn]]))

(deftest status-test
  (with-open [deps (frames/->system frames/app-cfg)]
    (let [request {:uri            "/status"
                   :request-method :get}
          handle (handler-fn deps)
          visit (-> (handle request)
                    (update :body slurp))]
      (is (= 200 (:status visit)))
      (is (= "{\"status\":\"OK\"}" (:body visit))))))
