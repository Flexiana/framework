(ns framework.route.core-test
  (:require
    [clojure.test :refer :all]
    [framework.route.core :as route]
    [framework.route.helpers :as helpers]
    [framework.state.core :as state]))

(def sample-request
  {:uri "/" :request-method :get})

(def sample-not-found-request
  {:uri "/not-found" :request-method :get})

(def sample-routes
  "Sample routes structure."
  {:routes [["/" {:action :action}]]})

(def sample-routes-with-handler
  "Sample routes structure."
  {:routes [["/" {:handler :handler}]]})

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
  (let [state (-> (state/make
                    (route/reset sample-routes)
                    sample-request)
                  route/match)
        ;; expected request data
        expected {:method :get
                  :match  #reitit.core.Match{:template    "/"
                                             :data        {:action :action}
                                             :result      nil
                                             :path-params {}
                                             :path        "/"}
                  :action :action}]
    ;; verify if updated request-data
    ;; is equal to the expected value
    (is (= expected (:request-data state)))))

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
