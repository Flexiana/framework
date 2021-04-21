(ns framework.components.runner-test
  (:require
    [clojure.test :refer :all]
    [framework.components.runner :refer [run]]
    [xiana.core :as xiana]))

(deftest runner-test
  (is (= {:enter/first true, :enter/second true, :action :action, :leave/second true, :leave/first true}
         (:right
           (xiana/flow->
             {}
             (run  [{:enter (fn [state] (xiana/ok (assoc state :enter/first true)))
                     :leave (fn [state] (xiana/ok (assoc state :leave/first true)))}
                    {:enter (fn [state] (xiana/ok (assoc state :enter/second true)))
                     :leave (fn [state] (xiana/ok (assoc state :leave/second true)))}]
                   #(xiana/ok (assoc % :action :action))))))))

(deftest error-test
  (is (= {:error "something wrong"}
         (:right
           (xiana/flow->
             {}
             (run [{:enter (fn [state] (xiana/ok (update state :num inc)))
                    :error (fn [state] (xiana/ok (assoc state :error "something wrong")))
                    :leave (fn [state] (xiana/ok (assoc state :num dec)))}]
                  #(xiana/ok (assoc % :action :action)))))))
  (is (= {:num 0, :action :action}
         (:right
           (xiana/flow->
             {:num 0}
             (run  [{:enter (fn [state] (xiana/ok (update state :num inc)))
                     :error (fn [state] (xiana/ok (assoc state :error "something wrong")))
                     :leave (fn [state] (xiana/ok (update state :num dec)))}]
                   #(xiana/ok (assoc % :action :action))))))))
