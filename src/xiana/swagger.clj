(ns xiana.swagger
  (:require
    [jsonista.core :as json]
    [xiana.route.helpers :as helpers]))

(defn routes->swagger-json [routes
                            & {type :type
                               render? :render?
                               route-opt-map :route-opt-map}]
  (-> routes
      helpers/routes->routes'
      ((if render?
         #(helpers/routes->swagger-data % :route-opt-map route-opt-map)
         identity))
      ((cond
         (= type :json) json/write-value-as-string
         (= type :edn) identity
         :else identity))))

(defn swagger-configs-there?
  "Checks if the config has the required keys for swagger functionality.
   Required keys:
   * :xiana/swagger
   * :xiana/swagger-ui"
  [config]
  (->> config
       ((juxt :xiana/swagger-ui :xiana/swagger))
       (reduce #(and % %2) true)
       boolean))

(defn ->swagger-data
  "Update routes for swagger data generation."
  [config & {routes :routes
             render? :render?
             type :type
             route-opt-map :route-opt-map}]
  (if (swagger-configs-there? config)
    (let [routes (or routes (-> config :routes))
          routes-swagger-data (routes->swagger-json routes
                                                    :type type
                                                    :render? render?
                                                    :route-opt-map route-opt-map)
          config-key (-> config
                         (get :xiana/swagger {})
                         (get :path :swagger.json))]
      (assoc config config-key routes-swagger-data))
    config))
