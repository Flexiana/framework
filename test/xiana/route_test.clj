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


