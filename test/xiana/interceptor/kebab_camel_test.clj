(ns xiana.interceptor.kebab-camel-test
  (:require [clojure.test :refer [deftest is testing]])
  (:require [xiana.interceptor.kebab-camel :as kc]))

(deftest req->kebab-resp->camel-test
  (testing "Transforms keys of request params to kebab case"
    (let [state {:request {:params {:paramKey1 1 :paramKey2 2 :paramKey3 3}}}
          expected {:request {:params {:param-key-1 1 :param-key-2 2 :param-key-3 3}}}
          enter (:enter kc/interceptor)
          result (enter state)]
      (is (= expected result))))

  (testing "Transform keys of response body to Camel case"
    (let [state {:response {:body {:param-key-1 1 :param-key-2 2 :param-key-3 3}}}
          expected {:response {:body {:paramKey1 1 :paramKey2 2 :paramKey3 3}}}
          leave (:leave kc/interceptor)
          result (leave state)]
      (is (= expected result)))))

