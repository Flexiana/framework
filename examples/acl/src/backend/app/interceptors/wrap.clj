(ns interceptors.wrap
  (:require
    [xiana.core :as xiana]))

(defn interceptor
  [in]
  (cond-> {}
    (:enter in) (assoc :enter (fn [state] (xiana/ok ((:enter in) state))))
    (:leave in) (assoc :leave (fn [state] (xiana/ok ((:leave in) state))))))

(defn middleware->enter
  ([middleware]
   (middleware->enter {} middleware))
  ([interceptor middleware]
   (assoc interceptor :enter (fn [{req :request :as state}]
                               (xiana/ok (assoc state :request ((middleware identity) req)))))))

(defn middleware->leave
  ([middleware]
   (middleware->leave {} middleware))
  ([interceptor middleware]
   (assoc interceptor :leave (fn [{req :request :as state}]
                               (xiana/ok (assoc state :request ((middleware identity) req)))))))

