(ns xiana.swagger
  (:require
    [clojure.string :as str]
    [hiccup.core :as h]
    [jsonista.core :as json]
    [malli.util]
    [meta-merge.core :refer [meta-merge]]
    [reitit.coercion :as coercion]
    [reitit.coercion.malli]
    [reitit.core :as r]
    [reitit.ring :as ring]
    [reitit.swagger]
    [reitit.trie :as trie]
    [ring.util.response]))

;; "xiana-route->reitit-route is taking route entry of our custom shape of routes
;; and transforms it into proper reitit route entry that is valid on the Swagger
;; implemention of reitit.

;; (xiana-route->reitit-route [\"/swagger-ui\" {:action :swagger-ui
;;                                              :some-values true}])
;; ;; => [\"/swagger-ui\"
;;        {:get
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :patch
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :trace
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :connect
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :delete
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :head
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :post
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :action :swagger-ui,
;;         :options
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :put
;;         {:handler #function[clojure.core/identity], :action :swagger-ui},
;;         :some-values true}]
;; "

(def all-methods
  [:get :patch :trace :connect :delete :head :post :options :put])

(defn- no-method?
  [opt-map]
  (:action opt-map))

(defn- reduce-opt-map
  [opt-map all-methods]
  (reduce (fn [acc method]
            (if (get acc method)
              (if (get-in acc [method :handler])
                acc
                (-> acc
                    (assoc-in [method :handler] identity)))
              acc))
          opt-map
          all-methods))

(defn- process-opt-map
  [opt-map all-methods]
  (if (no-method? opt-map)
    (-> opt-map
        (assoc-in [:get :handler] identity)
        (assoc-in [:get :action] (:action opt-map)))
    (reduce-opt-map opt-map all-methods)))

(defn xiana-route->reitit-route
  [[url opt-map & nested-routes :as route] all-methods]
  (when-not (-> route meta :no-doc)
    (apply conj
           [url (process-opt-map opt-map all-methods)]
           (map #(xiana-route->reitit-route % all-methods) nested-routes))))

(defn xiana-routes->reitit-routes
  "Transforms routes to the proper reitit form."
  [routes all-methods]
  (vec
    (keep #(xiana-route->reitit-route % all-methods) routes)))

(defn strip-top-level-keys
  [m]
  (dissoc m :id :info :host :basePath :definitions :securityDefinitions))

(def base-swagger-spec {:responses ^:displace {:default {:description ""}}})

(defn transform-endpoint
  [[method {{:keys [coercion no-doc swagger] :as data} :data
            middleware :middleware
            interceptors :interceptors}]]
  (when (and data (not no-doc))
    [method
     (meta-merge
       base-swagger-spec
       (apply meta-merge (keep (comp :swagger :data) middleware))
       (apply meta-merge (keep (comp :swagger :data) interceptors))
       (when coercion
         (coercion/get-apidocs coercion :swagger data))
       (select-keys data [:tags :summary :description])
       (strip-top-level-keys swagger))]))

(defn swagger-path
  [path opts]
  (-> path (trie/normalize opts) (str/replace #"\{\*" "{")))

(defn transform-path
  "Transform a path of a compiled route to swagger format."
  [[path _ api-verb-map] router]
  (when-let [endpoint (some->> api-verb-map (keep transform-endpoint) (seq) (into {}))]
    [(swagger-path path (r/options router)) endpoint]))

(defn routes->swagger-map
  "Creates the json representation of the routes "
  [routes & {route-opt-map :route-opt-map}]
  (let [router (ring/router routes (or route-opt-map {}))
        swagger {:swagger "2.0"
                 :x-id ::default
                 :info (get-in route-opt-map [:data :info])}
        map-in-order #(->> % (apply concat) (apply array-map))
        paths (->> router
                   (r/compiled-routes)
                   (map #(transform-path % router))
                   map-in-order)]
    (meta-merge swagger {:paths paths})))

#_(-> (config/config {:framework-edn-config "config/dev/config.edn"})
      ->default-internal-swagger-ui-html)

(defn ->default-internal-swagger-ui-html
  "Generate the html for swagger UI"
  [config]
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
// TODO: [@LeaveNhA] can be replace-able with in-app configuration and pass it with json-encoding
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

(defn- swagger-ui-endpoint
  [config]
  (let [{:keys [uri-path]} (get-in config [:xiana/swagger-ui])]
    ^{:no-doc true}
    [uri-path
     {:get {:action
            (fn [state]
              (assoc state
                     :response
                     (-> state
                         ->default-internal-swagger-ui-html
                         ring.util.response/response
                         (ring.util.response/header "Content-Type" "text/html; charset=utf-8"))))}}]))

(defn swagger-json-endpoint-action
  [state]
  (assoc state
         :response
         (-> (str (-> state :deps :swagger.json))
             ring.util.response/response
             (ring.util.response/header "Content-Type" "application/json; charset=utf-8"))))

(defn- swagger-json-endpoint
  [config]
  (let [{:keys [uri-path]} (get-in config [:xiana/swagger])]
    ^{:no-doc true}
    [uri-path
     {:action swagger-json-endpoint-action}]))

(defn swagger-dot-json
  "Create swagger.json for all methods for each endpoint"
  [routes & {type :type
             route-opt-map :route-opt-map}]
  (let [reitit-routes (xiana-routes->reitit-routes routes all-methods)
        swagger-map (routes->swagger-map reitit-routes :route-opt-map route-opt-map)]
    (cond
      (= type :json) (json/write-value-as-string swagger-map)
      (= type :edn)  swagger-map)))

(defn swagger-config?
  "Checks if the config has the required keys for swagger functionality.
   Required keys:
   * :xiana/swagger
   * :xiana/swagger-ui"
  [config]
  (every? some? ((juxt :xiana/swagger-ui :xiana/swagger) config)))

(defn add-swagger-endpoints
  "Takes the config and returns it with the swagger endpoints added"
  [config]
  (let [type :json
        config (update-in config [:xiana/swagger :data] eval)
        route-opt-map {:data (get-in config [:xiana/swagger :data])}
        config (assoc-in config [:xiana/swagger :data] route-opt-map)]
    (if (swagger-config? config)
      (let [routes (get config :routes)
            swagger-routes (apply conj routes [(swagger-ui-endpoint config) (swagger-json-endpoint config)])
            json-routes (swagger-dot-json swagger-routes
                                          :type type
                                          :route-opt-map route-opt-map)]
        (-> config
            (assoc :swagger.json json-routes)
            (assoc :routes swagger-routes)))
      config)))
