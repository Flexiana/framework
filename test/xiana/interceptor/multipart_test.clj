(ns xiana.interceptor.multipart-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [peridot.multipart :as p]
    [ring.mock.request :as mock]
    [xiana.interceptor :as interceptor])
  (:import (java.io File)))

(defn state []
  {:request
   (merge (mock/request :post "/upload")
          (p/build {:data (io/file "test/resources/multipart.csv")}))})

(deftest multipart-test
  (testing "Multipart support in the Framework interceptor chain"
    (let [f (:enter interceptor/params)
          r (f (state))
          data-params (get-in r [:request :params :data])]
      (is (= "multipart.csv" (:filename data-params)))
      (is (= "text/csv" (:content-type data-params)))
      (is (instance? File (:tempfile data-params)))
      (is (pos? (:size data-params)))
      (is (= "first_name,last_name,age\nJohn,Doe,25\nJane,Doe,24\nAlice,Smith,22\nBob,Johnson,30\n"
             (slurp (:tempfile data-params)))))))
