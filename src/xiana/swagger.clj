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

(defn swagger-configs-there? [config]
  "Checks if the config has the required keys for swagger functionality.
   Required keys:
   * :xiana/swagger
   * :xiana/swagger-ui"
  (->> config
       ((juxt :xiana/swagger-ui :xiana/swagger))
       (reduce #(and % %2) true)
       boolean))

(defn ->swagger-data
  "Update routes for swagger data generation."
  [config & {routes :routes}]
  (if (swagger-configs-there? config)
    (let [routes (or routes (-> config :routes))
         routes-swagger-data (routes->swagger-json routes
                                                   :type :json)
         config-key (-> config
                        (get :xiana/swagger {})
                        (get :path :swagger.json))]
     (assoc config config-key routes-swagger-data))
    config))
