(ns framework.route.core-test
  (:require
    [clojure.test :refer :all]
    [framework.route.core :as route]
    [framework.route.helpers :as helpers]
    [framework.state.core :as state]
    [xiana.core :as xiana]))

(defn request-data
  [router req]
  (:request-data (xiana/extract (router (state/make {} req)))))

(def sample-request
  {:uri "/" :request-method :get})

(def sample-not-found-request
  {:uri "/not-found" :request-method :get})

(def simple-routes
  "Sample routes structure."
  [["/" {:action :action}]])

(def complex-routes
  [["/api" {}
    ["/posts" {:get  {:action :do-get}
               :post {:action :do-post}}]]
   ["/todo" {:get {:action :get-all-todos}
             :put {:action :new-todo}}
    ["/:id" {:get    {:action :get-one-todo}
             :post   {:action :modify-todo}
             :delete {:action :delete-todo}}]]
   ["/assets/*" {:action :asset-handler}]])

(deftest contains-updated-request-data
  (let [router* (route/router simple-routes)
        expected {:action :action
                  :match  {:data        {:action :action}
                           :path-params {}}}]
    (is (= expected (request-data router* sample-request)))))

(deftest contains-not-found-action
  (let [router* (route/router simple-routes)
        state (state/make {} sample-not-found-request)]
    (is (thrown? Exception (router* state)))))

(deftest complex-routes-test-api
  (let [router* (route/router complex-routes)]
    (is (thrown? Exception
          (router* (state/make {} {:uri "/api" :request-method :get}))))
    (is (thrown? Exception (router*
                             (state/make
                               {}
                               {:uri "/api" :request-method :post}))))
    (is (= {:action :do-post
            :match  {:data        {:action :do-post}
                     :path-params {}}}
           (request-data router* {:uri "/api/posts" :request-method :post})))
    (is (= {:action :do-get
            :match  {:data        {:action :do-get}
                     :path-params {}}}
           (request-data router* {:uri "/api/posts" :request-method :get})))
    (is (thrown? Exception (router*
                             (state/make
                               {}
                               {:uri "/api/posts" :request-method :delete}))))))

(deftest complex-routes-test-todos
  (let [router* (route/router complex-routes)]
    (is (= {:action :get-all-todos
            :match  {:data        {:action :get-all-todos}
                     :path-params {}}}
           (request-data router* {:uri "/todo" :request-method :get})))
    (is (= {:action :new-todo
            :match  {:data        {:action :new-todo}
                     :path-params {}}}
           (request-data router* {:uri "/todo" :request-method :put})))
    (is (= {:action :get-one-todo
            :match  {:data        {:action :get-one-todo}
                     :path-params {:id "123"}}}
           (request-data router* {:uri "/todo/123" :request-method :get})))
    (is (= {:action :new-todo
            :match  {:data        {:action :new-todo}
                     :path-params {:id "123"}}}
           (request-data router* {:uri "/todo/123" :request-method :put})))))

(deftest assets-test
  (let [router* (route/router complex-routes)]
    (is (= {:action :asset-handler
            :match  {:data        {:action :asset-handler}
                     :path-params {:* "/123"}}}
           (request-data router* {:uri "/assets/123" :request-method :put})))
    (is (= {:action :asset-handler
            :match  {:data        {:action :asset-handler}
                     :path-params {:* "/123/456"}}}
           (request-data router* {:uri "/assets/123/456" :request-method :put})))))
