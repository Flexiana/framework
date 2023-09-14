(ns xiana.interceptor.cors)

(defn cors-headers [origin]
  {"Access-Control-Allow-Origin"      origin
   "Access-Control-Allow-Credentials" "true"
   "Access-Control-Allow-Methods"     "GET,PUT,POST,DELETE"
   "Access-Control-Allow-Headers"     "Origin, X-Requested-With, Content-Type, Accept"})

(defn preflight?
  "Returns true if the request is a preflight request"
  [request]
  (= (request :request-method) :options))

(def interceptor
  {:name  ::cross-origin-headers
   :leave (fn [state]
            (let [request (:request state)
                  headers (cors-headers (get-in state [:deps :cors-origin]))]
              (if (preflight? request)
                (update-in state [:response]
                           merge
                           {:status  200
                            :headers headers
                            :body    "preflight complete"})
                (update-in state [:response :headers]
                           merge headers))))})
