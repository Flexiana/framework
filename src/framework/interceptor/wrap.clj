(ns framework.interceptor.wrap
  "To wrap any kind of middlewares and interceptors to xiana flow."
  (:require
    [xiana.core :as xiana]))

(defn interceptor
  "Interceptor wrapper to use xiana monad."
  [in]
  (cond-> {}
    (:enter in) (assoc :enter (fn [state] (xiana/ok ((:enter in) state))))
    (:leave in) (assoc :leave (fn [state] (xiana/ok ((:leave in) state))))
    (:error in) (assoc :error (fn [state] (xiana/error ((:error in) state))))))

(defn- middleware-fn
  "Simple enter/leave middleware function generator."
  [m k]
  (fn [{r k :as state}]
    (xiana/ok (-> state (assoc k (m r))))))

(defn middleware->enter
  "Parse middleware function to interceptor enter lambda function."
  ([middleware]
   (middleware->enter {} middleware))
  ([interceptor middleware]
   (let [m (middleware identity)
         f (middleware-fn m :request)]
     (-> interceptor (assoc :enter f)))))

(defn middleware->leave
  "Parse middleware function to interceptor leave lambda function."
  ([middleware]
   (middleware->leave {} middleware))
  ([interceptor middleware]
   (let [m (middleware identity)
         f (middleware-fn m :response)]
     (-> interceptor (assoc :leave f)))))
