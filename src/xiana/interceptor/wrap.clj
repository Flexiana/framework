(ns xiana.interceptor.wrap
  "To wrap any kind of middlewares and interceptors to xiana flow.")

(defn- middleware-fn
  "Simple enter/leave middleware function generator."
  [m k]
  (fn [{r k :as state}]
    (assoc state k (m r))))

(defn middleware->enter
  "Parse middleware function to interceptor enter lambda function."
  ([middleware]
   (middleware->enter {} middleware))
  ([interceptor middleware]
   (let [m (middleware identity)
         f (middleware-fn m :request)]
     (assoc interceptor :enter f))))

(defn middleware->leave
  "Parse middleware function to interceptor leave lambda function."
  ([middleware]
   (middleware->leave {} middleware))
  ([interceptor middleware]
   (let [m (middleware identity)
         f (middleware-fn m :response)]
     (assoc interceptor :leave f))))
