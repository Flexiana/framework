(ns xiana.swagger
  (:require
   [jsonista.core :as json]
   [xiana.route.helpers :as helpers]))

(defn routes->swagger-json [routes & {type :type}]
  (-> routes
      helpers/routes->routes'
      helpers/routes->swagger-data
      ((cond
         (= type :json) json/write-value-as-string
         (= type :edn) identity
         :else identity))))

(defn ->swagger-data
  "Update routes."
  [config]
  (let [routes-swagger-data (-> config
                                :routes
                                .routes
                                (routes->swagger-json :type json))]
    (assoc config :swagger-data routes-swagger-data)))
