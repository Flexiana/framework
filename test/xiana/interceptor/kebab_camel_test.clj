(ns xiana.interceptor.kebab-camel-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [xiana.interceptor.kebab-camel :as kc]))

(deftest req->kebab-resp->camel-test
  (testing "Transforms keys of request params to kebab case"
    (let [state    {:request {:params           {:paramKey1 1 :paramKey2 2 :paramKey3 3}
                              :body-params      {:bodyParamKey1 1 :bodyParamKey2 2 :bodyParamKey3 3}
                              :query-params     {:queryParamKey1 1 :queryParamKey2 2 :queryParamKey3 3}
                              :path-params      {:pathParamKey1 1 :pathParamKey2 2 :pathParamKey3 3}
                              :form-params      {:formParamKey1 1 :formParamKey2 2 :formParamKey3 3}
                              :multipart-params {:multipartParamKey1 1 :multipartParamKey2 2 :multipartParamKey3 3}}}
          expected {:request {:params           {:param-key-1 1 :param-key-2 2 :param-key-3 3}
                              :body-params      {:body-param-key-1 1 :body-param-key-2 2 :body-param-key-3 3}
                              :query-params     {:query-param-key-1 1 :query-param-key-2 2 :query-param-key-3 3}
                              :path-params      {:path-param-key-1 1 :path-param-key-2 2 :path-param-key-3 3}
                              :form-params      {:form-param-key-1 1 :form-param-key-2 2 :form-param-key-3 3}
                              :multipart-params {:multipart-param-key-1 1 :multipart-param-key-2 2 :multipart-param-key-3 3}}}
          enter    (:enter kc/interceptor)
          result   (enter state)]
      (is (= expected result))))

  (testing "Transform keys of response body to Camel case"
    (let [state    {:response {:body {:param-key-1 1 :param-key-2 2 :param-key-3 3}}}
          expected {:response {:body {:paramKey1 1 :paramKey2 2 :paramKey3 3}}}
          leave    (:leave kc/interceptor)
          result   (leave state)]
      (is (= expected result)))))

