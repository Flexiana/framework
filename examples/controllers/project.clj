(defproject controllers "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.flexiana/framework "0.3.0"]
                 [metosin/reitit "0.5.15"]
                 [metosin/malli "0.6.2"]
                 [duct/server.http.jetty "0.2.1"]
                 [thheller/shadow-cljs "2.11.7"]
                 [reagent "0.10.0"]
                 [clj-http "3.12.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [re-frame "1.1.2"]]
  :plugins [[lein-shadow "0.3.1"]
            [lein-shell "0.5.0"]
            [migratus-lein "0.7.3"]]
  :main ^:skip-aot core
  :uberjar-name "frames.jar"
  :source-paths ["src/backend/app" "src/backend/components" "src/frontend" "src/shared"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :profiles {:dev   {:resource-paths ["config/dev"]
                     :dependencies   [[binaryage/devtools "1.0.2"]]}
             :local {:resource-paths ["config/local"]}
             :prod  {:resource-paths ["config/prod"]}
             :test  {:resource-paths ["config/test"]
                     :dependencies   [[kerodon "0.9.1"]]}}
  :jvm-opts ["-Dmalli.registry/type=custom"]
  :shadow-cljs {:nrepl {:port 8777}

                :builds {:app {:target     :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules    {:app {:init-fn  controllers.core/init
                                                  :preloads [devtools.preload]}}}}}
                                        ;:devtools {:http-root "resources/public"
                                        ;           :http-port 8280
                                        ;           :http-handler controllers.handler/dev-handler
                                        ;           }


  :aliases {"ci"      ["do" "clean," "cloverage," "lint," "uberjar"]
            "kondo"   ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]
            "lint"    ["do" "kondo," "eastwood," "kibit"]
            "watch"   ["with-profile" "dev" "do"
                       ["shadow" "watch" "app" "browser-test" "karma-test"]]
            "release" ["with-profile" "prod" "do"
                       ["shadow" "release" "app"]]}
  :migratus {:store         :database
             :migration-dir "migrations"
             :db            {:classname   "com.mysql.jdbc.Driver"
                             :subprotocol "postgres"
                             :subname     "//localhost/controllers"
                             :user        "postgres"
                             :password    "postgres"}})
