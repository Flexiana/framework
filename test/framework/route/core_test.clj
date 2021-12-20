(ns framework.route.core-test
  (:require
    [clojure.test :refer :all]
    [framework.route.core :as route]
    [framework.route.helpers :as helpers]
    [framework.state.core :as state]
    [xiana.core :as xiana]))

(def sample-request
  {:uri "/" :request-method :get})

(def sample-not-found-request
  {:uri "/not-found" :request-method :get})

(def sample-routes
  "Sample routes structure."
  [["/" {:action :action}]])

(deftest contains-updated-request-data
  (let [router* (route/router sample-routes)
        state (state/make {} sample-request)
        expected {:action :action
                  :match  {:data        {:action :action}
                           :path-params {}}}]
    (is (= expected (:request-data (xiana/extract (router* state)))))))

(deftest contains-not-found-action
  (let [router* (route/router sample-routes)
        state (state/make {} sample-not-found-request)]
    (is (thrown? Exception (router* state)))))
