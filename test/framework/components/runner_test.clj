(ns framework.components.runner-test
  (:require
    [clojure.test :refer :all]
    [framework.components.runner :refer [run]]))

(deftest runner-test
  (is (= {:enter/first true
          :enter/second true
          :action :action
          :leave/second true
          :leave/first true}
         (run {} [{:enter (fn [state] (assoc state :enter/first true))
                   :leave (fn [state] (assoc state :leave/first true))}
                  {:enter (fn [state] (assoc state :enter/second true))
                   :leave (fn [state] (assoc state :leave/second true))}]
              #(assoc % :action :action)))))

(deftest error-test
  (is (= {:error "something wrong"}
         (run {} [{:enter (fn [state] (update state :num inc))
                   :error (fn [state] (assoc state :error "something wrong"))
                   :leave (fn [state] (assoc state :num dec))}]
              #(assoc % :action :action))))
  (is (= {:num 0, :action :action}
         (run {:num 0} [{:enter (fn [state] (update state :num inc))
                         :error (fn [state] (assoc state :error "something wrong"))
                         :leave (fn [state] (update state :num dec))}]
              #(assoc % :action :action)))))
