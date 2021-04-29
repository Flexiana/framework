(ns framework.components.interceptors.wrap
  (:require
    [xiana.core :as xiana]))

(defn interceptor
  "Interceptor wrapper to use xiana monad"
  [in]
  (cond-> {}
    (:enter in) (assoc :enter (fn [state] (xiana/ok ((:enter in) state))))
    (:leave in) (assoc :leave (fn [state] (xiana/ok ((:leave in) state))))
    (:error in) (assoc :error (fn [state] (xiana/error ((:error in) state))))))

(defn middleware->enter
  "add middleware as :enter of interceptor"
  ([middleware]
   (middleware->enter {} middleware))
  ([interceptor middleware]
   (assoc interceptor :enter (fn [{req :request :as state}]
                               (xiana/ok (assoc state :request ((middleware identity) req)))))))

(defn middleware->leave
  "add middleware as :leave of interceptor"
  ([middleware]
   (middleware->leave {} middleware))
  ([interceptor middleware]
   (assoc interceptor :leave (fn [{res :response :as state}]
                               (xiana/ok (assoc state :response ((middleware identity) res)))))))
