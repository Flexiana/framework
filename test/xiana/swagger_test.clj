(ns xiana.swagger-test
  (:require [xiana.swagger :as sut]
            [clojure.test :as t :refer [deftest is testing]]))

(def sample-routes
  "Sample routes structure."
  {:routes [["/" {:action :action}]]})

(def sample-routes-with-handler
  "Sample routes structure."
  {:routes [["/" {:handler :handler}]]})

(def sample-routes-without-action
  "Sample routes structure (without action or handler)."
  {:routes [["/" {}]]})

(deftest swagger-route-data-generation
  (testing "Swagger Data generation from Routes\n"
    (testing "Swagger Data from Empty Route"
      (let [generated (-> []
                          :routes
                          (sut/swagger-dot-json :type :edn)
                          :paths)
            count-of-generated-routes-data (count generated)]
        (is generated)
        (is (zero? count-of-generated-routes-data))))

    (testing "Swagger Data from Sample Route /w handle\n"
      (let [generated-swagger-data (-> sample-routes-with-handler
                                       :routes
                                       (sut/swagger-dot-json :type :edn))]
        (testing "One swagger route for one route entry?"
          (let [generated-route-count (-> generated-swagger-data
                                          :paths
                                          count)]
            (is (= generated-route-count 1))))
        (testing "Actions should generate every methods"
          (let [index-generated-methods-by-sample (->
                                                   generated-swagger-data
                                                   :paths
                                                   (get "/")
                                                   keys
                                                   set)]
            (is (= index-generated-methods-by-sample
                   (set sut/all-methods)))))))

    (testing "Swagger Data from Sample Route /w action"
      (let [generated-swagger-data (-> sample-routes
                                       :routes
                                       (sut/swagger-dot-json :type :edn))]
        (testing "One swagger route for one route entry?"
          (let [generated-route-count (->
                                       generated-swagger-data
                                       :paths
                                       keys
                                       count)]
            (is (= generated-route-count
                   1))))
        (testing "Actions should generate every methods"
          (let [index-generated-methods-by-sample (->
                                                   generated-swagger-data
                                                   :paths
                                                   (get "/")
                                                   keys
                                                   set)]
            (is (=
                 index-generated-methods-by-sample
                 (set sut/all-methods)))))))))





(comment
;  (def all-methods [:get :post])
  ;; (deftest routes
  ;;   (testing "single-route"
  ;;     (= ["/"
  ;;         {:action :swagger-ui
  ;;          :some-values true
  ;;          :get
  ;;          {:handler #'identity :action :swagger-ui}
  ;;          :post
  ;;          {:handler #'identity :action :swagger-ui}}]
  ;;        (sut/xiana-route->reitit-route
  ;;         all-methods
  ;;         ["/" {:action :swagger-ui
  ;;               :some-values true}]))))
  )
