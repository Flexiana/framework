(ns asset-test
  (:require
    [clojure.test :refer :all]
    [components]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]))

(deftest status-test
  (let [deps (components/deps (config/env))
        request {:uri            "/assets/Clojure-icon.png"
                 :request-method :get}
        handle (handler-fn deps)
        visit (-> (handle request)
                  (update :body slurp))]
    (is (= 200 (:status visit)))
    (is (= (slurp "resources/public/assets/Clojure-icon.png")
           (:body visit)))))
