(ns asset-test
  (:require
    [clojure.test :refer :all]
    [components]
    [framework.handler.core :refer [handler-fn]]))

(deftest status-test
  (with-open [deps (components/->system components/app-cfg)]
    (let [request {:uri            "/assets/Clojure-icon.png"
                   :request-method :get}
          handle (handler-fn deps)
          visit (-> (handle request)
                    (update :body slurp))]
      (is (= 200 (:status visit)))
      (is (= (slurp "resources/public/assets/Clojure-icon.png")
             (:body visit))))))
