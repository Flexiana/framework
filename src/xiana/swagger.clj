(ns xiana.swagger
  (:require
   [clojure.string :as str]
   [jsonista.core :as json]
   [meta-merge.core :refer [meta-merge]]
   [reitit.coercion :as rcoercion]
   [reitit.core :as r]
   [reitit.ring :as ring]
   [reitit.trie :as trie]
   [ring.util.response]
   [reitit.coercion.malli]
   [malli.util]
   [reitit.swagger]
   [hiccup.core :as h]))

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

(defn ->default-internal-swagger-ui-html [config]
  (let [schema-protocol (get-in config [:deps :xiana/web-server :protocol] :http)
        swagger-json-uri-path (get-in config [:deps :xiana/swagger :uri-path])]
    (h/html [:html {:lang "en"}
             [:head
              [:meta {:charset "UTF-8"}]
              [:title "Swagger UI"]
              [:link
               {:referrerpolicy "no-referrer",
                :crossorigin "anonymous",
                :integrity
                "sha512-lfbw/3iTOqI2s3gVb0fIwex5Y9WpcFM8Oq6XMpD8R5jMjOgzIgXjDNg7mNqbWS1I6qqC7sFaaMHXNsnVstkQYQ==",
                :href
                "https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/3.52.4/swagger-ui.min.css",
                :rel "stylesheet"}]
              [:style
               "html {box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll;}
               *, *:before, *:after { box-sizing: inherit;}
               body {margin: 0; background: #fafafa;}"]
              [:link
               {:sizes "32x32",
                :href "./favicon-32x32.png",
                :type "image/png",
                :rel "icon"}]
              [:link
               {:sizes "16x16",
                :href "./favicon-16x16.png",
                :type "image/png",
                :rel "icon"}]]
             [:body
              [:div#swagger-ui]
              [:script
               {:referrerpolicy "no-referrer",
                :crossorigin "anonymous",
                :integrity
                "sha512-w+D7rGMfhW/r7/lGU7mu92gjvuo4ZQddFOm5iJ0EAQNS7mmhCb10I8GcgrGTr1zJvCYcxj4roHMo66sLNQOgqA==",
                :src
                "https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/3.52.4/swagger-ui-bundle.min.js"}]
              [:script
               {:referrerpolicy "no-referrer",
                :crossorigin "anonymous",
                :integrity
                "sha512-OdiS0y42zD5WmBnJ6H8K3SCYjAjIJQrUOAraBx5PH1QSLtq+KNLy80uQKruXCJTGZKdQ7hhu/AD+WC+wuYUS+w==",
                :src
                "https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/3.52.4/swagger-ui-standalone-preset.min.js"}]
              [:script
               (str "window.onload = function ()
{
// TODO: [Seçkin] can be replace-able with in-app configuration and pass it with json-encoding
    window.ui = SwaggerUIBundle(
        {
            url: '" swagger-json-uri-path "',
            schemes: ['" (name schema-protocol) "'],
            dom_id: '#swagger-ui',
            deepLinking: true,
            presets: [SwaggerUIBundle.presets.apis,
                      SwaggerUIStandalonePreset],
            plugins: [SwaggerUIBundle.plugins.DownloadUrl],
            layout: 'StandaloneLayout'}
    );
};")]]])))

#_(-> (config/config {:framework-edn-config "config/dev/config.edn"})
      ->default-internal-swagger-ui-html)

(defn ->default-internal-swagger-endpoints [config]
  [(let [{:keys [uri-path]} (get-in config [:xiana/swagger-ui])]
     ^{:no-doc true}
     [uri-path
      {:get {:action
             (fn [state]
               (assoc state
                      :response
                      (-> state
                          ->default-internal-swagger-ui-html
                          ring.util.response/response
                          (ring.util.response/header "Content-Type" "text/html; charset=utf-8"))))}}])
   (let [{:keys [uri-path]} (get-in config [:xiana/swagger])]
     ^{:no-doc true}
     [uri-path
      {:action (fn [state]
                 (assoc state
                        :response
                        (-> (str (-> state :deps ((get-in state [:deps :xiana/swagger :path]))))
                            ring.util.response/response
                            (ring.util.response/header "Content-Type" "application/json; charset=utf-8"))))}])])

(defn routes->swagger-json [routes
                            & {type :type
                               render? :render?
                               route-opt-map :route-opt-map}]
  (-> routes
      routes->routes'
      ((if (not (false? render?))
         #(routes->swagger-data % :route-opt-map route-opt-map)
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
  (every? some? ((juxt :xiana/swagger-ui :xiana/swagger) config)))

(defn ->swagger-data
  "Update routes for swagger data generation."
  [config & {routes :routes
             internal? :internal?
             render? :render?
             type :type
             route-opt-map :route-opt-map}]
  (let [internal? (or internal? true)
        render? (or render? true)
        type (or type :json)
        config (update-in config [:xiana/swagger :data] eval)
        route-opt-map {:data (or (get-in config [:xiana/swagger :data])
                                 route-opt-map)}
        config (update-in config [:xiana/swagger :data] (constantly route-opt-map))]
    (if (swagger-configs-there? config)
      (let [routes (or routes
                       (get config :routes []))
            routes (if internal?
                     (apply conj routes (->default-internal-swagger-endpoints config))
                     routes)
            routes-swagger-data (routes->swagger-json routes
                                                      :type type
                                                      :render? render?
                                                      :route-opt-map route-opt-map)
            #_"TODO: [Seçkin] You can't just place not-found value."
            config-key (get-in config [:xiana/swagger :path] :swagger.json)]
        (-> config
            (assoc config-key routes-swagger-data)
            (assoc :routes routes)))
      config)))
