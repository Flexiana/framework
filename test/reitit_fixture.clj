(ns reitit-fixture
  (:require
    [clj-http.client :as client]
    [clojure.java.io :as io]
    [clojure.test :refer [is deftest testing]]
    [com.stuartsierra.component :as component]
    [framework.components.interceptors :as interceptors]
    [framework.components.session.backend :as session-backend]
    [framework.components.session.interceptors :refer [init-in-memory-interceptor]]
    [framework.components.web-server.core :refer [->reitit-web-server
                                                  reitit-handler]]
    [framework.config.core :as config]
    [framework.db.storage :refer [->postgresql]]
    [matcher-combinators.test :refer [match?]]
    [muuntaja.core :as m]
    [muuntaja.interceptor :as minterceptor]
    [next.jdbc :as jdbc]
    [reitit.coercion.malli :as rcm]
    [reitit.dev.pretty :as pretty]
    [reitit.http :as http]
    [reitit.http.coercion :as rhc]
    [reitit.http.interceptors.exception :as exception]
    ; interceptor mode in reitit is called http
    [reitit.http.interceptors.multipart :as multipart]
    [reitit.http.interceptors.muuntaja :as muuntaja]
    [reitit.http.interceptors.parameters :as parameters]
    [reitit.interceptor.sieppari :as sieppari]
    [reitit.ring :as ring]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [ring.adapter.jetty :as jetty]
    [xiana.core :as xiana])
  (:import
    (com.opentable.db.postgres.embedded
      EmbeddedPostgres)
    (java.lang
      AutoCloseable)))

(def sys-deps
  {:web-server [:db]})

(def router-config
  {:exception pretty/exception
   :data      {:coercion     rcm/coercion
               :muuntaja     m/instance
               :interceptors [swagger/swagger-feature
                              (init-in-memory-interceptor)
                              (parameters/parameters-interceptor)
                              (muuntaja/format-negotiate-interceptor)
                              (muuntaja/format-response-interceptor)
                              (exception/exception-interceptor)
                              (muuntaja/format-request-interceptor)
                              (multipart/multipart-interceptor)
                              (rhc/coerce-response-interceptor)
                              (rhc/coerce-exceptions-interceptor)
                              (rhc/coerce-request-interceptor)]}})

(defn embedded-postgres!
  [config]
  (let [pg        (.start (EmbeddedPostgres/builder))
        pg-port   (.getPort pg)
        nuke-sql  (slurp "./Docker/init.sql")
        db-config (-> config
                      :framework.db.storage/postgresql
                      (assoc
                        :port pg-port
                        :embedded pg
                        :subname (str "//localhost:" pg-port "/framework")))]
    (jdbc/execute! (dissoc db-config :dbname) [nuke-sql])
    (assoc config :framework.db.storage/postgresql db-config)))

(defn system
  [config router-config routes]
  {:db         (->postgresql config)
   :web-server (->reitit-web-server config router-config routes)})

(defn test-system
  [routes]
  (-> (config/edn)
      embedded-postgres!
      (system router-config routes)
      component/map->SystemMap
      (component/system-using sys-deps)))

(defn std-system-fixture
  [f]
  (let [st (atom nil)]
    (reset! st (component/start (system routes)))
    (try
      (f)
      (finally
        (swap! st component/stop)))))

(deftest swagger-test
  (let [routes (fn [_config]
                 [["/swagger.json"
                   {:get {:no-doc  true
                          :swagger {:info {:title       "my-api"
                                           :description "with reitit-http"}
                                    :tags [{:name        "hello"
                                            :description "hello endpoint"}]}
                          :handler (swagger/create-swagger-handler)}}]
                  ["/hello" {:get {:handler (fn [_]
                                              {:status 200
                                               :body   "Hi"})}}]])
        system (test-system routes)
        st     (atom nil)]
    (reset! st (component/start system))

    (testing "Api-docs"
      (is (= {:status 200,
              :body
              "<!-- HTML for static distribution bundle build -->\n<!DOCTYPE html>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"UTF-8\">\n    <title>Swagger UI</title>\n    <link rel=\"stylesheet\" type=\"text/css\" href=\"./swagger-ui.css\" >\n    <link rel=\"icon\" type=\"image/png\" href=\"./favicon-32x32.png\" sizes=\"32x32\" />\n    <link rel=\"icon\" type=\"image/png\" href=\"./favicon-16x16.png\" sizes=\"16x16\" />\n    <style>\n      html\n      {\n        box-sizing: border-box;\n        overflow: -moz-scrollbars-vertical;\n        overflow-y: scroll;\n      }\n\n      *,\n      *:before,\n      *:after\n      {\n        box-sizing: inherit;\n      }\n\n      body\n      {\n        margin:0;\n        background: #fafafa;\n      }\n    </style>\n  </head>\n\n  <body>\n    <div id=\"swagger-ui\"></div>\n\n    <script src=\"./swagger-ui-bundle.js\" charset=\"UTF-8\"> </script>\n    <script src=\"./swagger-ui-standalone-preset.js\" charset=\"UTF-8\"> </script>\n    <script>\n    window.onload = function() {\n      // Begin Swagger UI call region\n      const ui = SwaggerUIBundle({\n        url: \"https://petstore.swagger.io/v2/swagger.json\",\n        dom_id: '#swagger-ui',\n        deepLinking: true,\n        presets: [\n          SwaggerUIBundle.presets.apis,\n          SwaggerUIStandalonePreset\n        ],\n        plugins: [\n          SwaggerUIBundle.plugins.DownloadUrl\n        ],\n        // Provided my ring-swagger\n        configUrl: \"./config.json\",\n        layout: \"StandaloneLayout\"\n      })\n      // End Swagger UI call region\n\n      window.ui = ui\n    }\n  </script>\n  </body>\n</html>\n"}
             (-> {:method :get
                  :url    "http://localhost:3000/api-docs"}
                 client/request
                 (select-keys [:status :body])))))

    (testing "swagger.json"
      (is (= {:status 200,
              :body   {:tags    [{:description "hello endpoint", :name "hello"}],
                       :paths
                       {(keyword "/hello") {:get {:produces   ["application/json"
                                                               "application/transit+msgpack"
                                                               "application/transit+json"
                                                               "application/edn"],
                                                  :consumes   ["application/json"
                                                               "application/transit+msgpack"
                                                               "application/transit+json"
                                                               "application/edn"],
                                                  :responses  {:default {:description ""}},
                                                  :parameters []}}},
                       :info    {:description "with reitit-http", :title "my-api"},
                       :swagger "2.0",
                       :x-id    ["reitit.swagger/default"]}}
             (-> {:method :get
                  :url    "http://localhost:3000/swagger.json"}
                 client/request
                 (select-keys [:status :body])
                 (update :body (partial m/decode "application/json"))))))

    (swap! st component/stop)))

#_(deftest )
