(ns xiana.route.helpers
  "The default not found and action functions"
  (:require
    [clojure.string :as str]
    [meta-merge.core :refer [meta-merge]]
    [reitit.coercion :as rcoercion]
    [reitit.core :as r]
    [reitit.ring :as ring]
    [reitit.trie :as trie]))

(defn not-found
  "Default not-found response handler helper."
  [state]
  (assoc state :response {:status 404 :body "Not Found"}))

(defn unauthorized
  "Default unauthorized response handler helper."
  [state msg]
  (assoc state :response {:status 401 :body msg}))

(defn action
  "Default action response handler helper."
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (assoc state :response (handler request))
    (catch Exception e
      (-> state
          (assoc :error e)
          (assoc :response
                 {:status 500 :body "Internal Server error"})))))

(defonce all-methods
  [:get :patch :trace :connect :delete :head :post :options :put])

(def routes->routes'
  #(-> (fn *** [[url opt-map & nested-routes :as route]]
         (let [new-opt-map (if (:action opt-map)
                             (let [action' (:action opt-map)
                                   swagger-base-of-endpoint (:swagger-* opt-map)]
                               (reduce (fn [acc method]
                                         (-> acc
                                             (assoc-in [method :handler] identity)
                                             (assoc-in [method :action] action')
                                             (merge swagger-base-of-endpoint)))
                                       opt-map
                                       all-methods))
                             (let [swagger-base-of-endpoint (get opt-map :swagger-* {})]
                               (reduce (fn [acc method]
                                         (if (get acc method)
                                           (if (get-in acc [method :handler])
                                             acc
                                             (-> acc
                                                 (assoc-in [method :handler] identity)
                                                 (merge swagger-base-of-endpoint)))
                                           acc))
                                       opt-map
                                       all-methods)))]
           (if (-> route meta :no-doc)
             nil
             (apply conj [url new-opt-map]
                    (map *** nested-routes)))))
       (keep %)
       vec))

(defn routes->swagger-data [routes' & {route-opt-map :route-opt-map}]
  (let [request-method :get
        routes' (-> routes' routes->routes' (ring/router (or route-opt-map {})))
        {:keys [id] :or {id ::default} :as swagger} (-> routes' :result request-method :data :swagger)
        ids (trie/into-set id)
        strip-top-level-keys #(dissoc % :id :info :host :basePath :definitions :securityDefinitions)
        strip-endpoint-keys #(dissoc % :id :parameters :responses :summary :description)
        swagger (->> (strip-endpoint-keys swagger)
                     (merge {:swagger "2.0"
                             :x-id ids}))
        swagger-path (fn [path opts]
                       (-> path (trie/normalize opts) (str/replace #"\{\*" "{")))
        base-swagger-spec {:responses ^:displace {:default {:description ""}}}
        transform-endpoint (fn [[method {{:keys [coercion no-doc swagger] :as data} :data
                                         middleware :middleware
                                         interceptors :interceptors}]]
                             (when (and data (not no-doc))
                               [method
                                (meta-merge
                                  base-swagger-spec
                                  (apply meta-merge (keep (comp :swagger :data) middleware))
                                  (apply meta-merge (keep (comp :swagger :data) interceptors))
                                  (when coercion
                                    (rcoercion/get-apidocs coercion :swagger data))
                                  (select-keys data [:tags :summary :description])
                                  (strip-top-level-keys swagger))]))
        transform-path (fn [[p _ c]]
                         (when-let [endpoint (some->> c (keep transform-endpoint) (seq) (into {}))]
                           [(swagger-path p (r/options routes')) endpoint]))
        map-in-order #(->> % (apply concat) (apply array-map))
        paths (->> routes' (r/compiled-routes)
                   ;; (filter accept-route)
                   ;; (map transform-endpoint)
                   (map transform-path)
                   map-in-order)]
    (meta-merge swagger {:paths paths})))
