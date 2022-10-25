(ns xiana.route-test
  (:require
    [clojure.test :refer :all]
    [xiana.route :as route]
    [xiana.route.helpers :as helpers]
    [xiana.state :as state]
    [xiana.swagger :as xsw]))

(def sample-request
  {:uri "/" :request-method :get})

(def sample-not-found-request
  {:uri "/not-found" :request-method :get})

(def sample-routes
  "Sample routes structure."
  {:routes [["/" {:action :action}]]})

(def sample-routes-with-no-doc
  "Sample routes structure with no-documentation meta flag."
  {:routes [^{:no-doc true} ["/" {:action :action}]]})

(def sample-routes-with-handler
  "Sample routes structure."
  {:routes [["/" {:handler :handler}]]})

(def sample-routes-with-handler-and-no-doc
  "Sample routes structure with no-documentation meta flag."
  {:routes [^{:no-doc true} ["/" {:handler :handler}]]})

(def sample-routes-without-action
  "Sample routes structure (without action or handler)."
  {:routes [["/" {}]]})

;; test reset routes functionality
(deftest contains-sample-routes
  (let [routes (route/reset sample-routes)]
    (is (= (:routes sample-routes) (.routes (:routes routes))))))

;; test route match update request-data (state) functionality
(deftest contains-updated-request-data
  ;; get state from sample request micro/match flow
  (let [state (route/match (state/make
                             (route/reset sample-routes)
                             sample-request))
        ;; expected request data
        expected {:method :get
                  :match  #{[:data {:action :action}]
                            [:path "/"]
                            [:path-params {}]
                            [:result nil]
                            [:template "/"]}
                  :action :action}]
    ;; verify if updated request-data
    ;; is equal to the expected value
    (is (= expected (-> state
                        :request-data
                        (update :match set))))))

;; test if the updated request-data (state) data handles the
(deftest contains-not-found-action
  ;; get action from sample request micro/match flow
  (let [action (-> (state/make
                     (route/reset sample-routes)
                     sample-not-found-request)
                   route/match
                   :request-data
                   :action)
        ;; expected action
        expected helpers/not-found]
    ;; verify if action has the expected value
    (is (= action expected))))

;; test if the updated request-data contains the right action
(deftest route-contains-default-action
  ;; get action from the updated state/match (micro) flow computation
  (let [action (-> (state/make
                     (route/reset sample-routes-with-handler)
                     sample-request)
                   route/match
                   :request-data
                   :action)
        ;; expected action
        expected helpers/action]
    ;; verify if action has the expected value
    (is (= action expected))))

;; test if the route/match flow handles a route without a handler or action
(deftest handles-route-without-action-or-handler
  ;; get action from the updated state/match (micro) flow computation
  (let [action (-> (state/make
                     (route/reset sample-routes-without-action)
                     sample-request)
                   route/match
                   :request-data
                   :action)
        ;; expected action? TODO: research
        expected helpers/not-found]
    ;; verify if action has the expected value
    (is (= action expected))))

(deftest swagger-route-data-generation
  (testing "Swagger Data generation from Routes"
    (testing "Swagger Data from Empty Route"
      (let [generated (-> sample-routes-without-action
                          :routes
                          (xsw/routes->swagger-json :type :edn))
            count-of-generated-routes-data (-> generated
                                               :paths
                                               keys
                                               count)]
        (is generated)
        (is (zero? count-of-generated-routes-data))))
    (testing "Swagger Data from Sampel Route /w handler"
      (let [generated-swagger-data (-> sample-routes-with-handler
                                       :routes
                                       (xsw/routes->swagger-json :type :edn))]
        (testing "One swagger route for one route entry?"
          (let [generated-route-count (->>
                                        generated-swagger-data
                                        :paths
                                        keys
                                        count)]
            (is (= generated-route-count
                   1))))
        (testing "Actions should generate every methods"
          (let [index-generated-methods-by-sample (->>
                                                    generated-swagger-data
                                                    :paths
                                                    (#(get % "/"))
                                                    keys
                                                    vec)]
            (is (=
                  index-generated-methods-by-sample
                  helpers/all-methods))))))
    (testing "Swagger Data from Sample Route /w action"
      (let [generated-swagger-data (-> sample-routes
                                       :routes
                                       (xsw/routes->swagger-json :type :edn))]
        (testing "One swagger route for one route entry?"
          (let [generated-route-count (->>
                                        generated-swagger-data
                                        :paths
                                        keys
                                        count)]
            (is (= generated-route-count
                   1))))
        (testing "Actions should generate every methods"
          (let [index-generated-methods-by-sample (->>
                                                    generated-swagger-data
                                                    :paths
                                                    (#(get % "/"))
                                                    keys
                                                    vec)]
            (is (=
                  index-generated-methods-by-sample
                  helpers/all-methods))))))))
