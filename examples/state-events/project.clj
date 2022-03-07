(defproject state-events "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.11.4"]
                 [com.flexiana/framework "0.4.0-rc3"]
                 [thheller/shadow-cljs "2.17.7"]
                 [org.clojure/tools.namespace "1.2.0"]
                 [tick "0.5.0-RC5"]]
  :plugins [[lein-shadow "0.4.0"]]
  :main ^:skip-aot state-events.core
  :uberjar-name "state-events.jar"
  :source-paths ["src/backend" "src/frontend" "src/shared"]
  :clean-targets ^{:protect false} ["resources/public/assets/js/compiled" "target"]
  :profiles {:dev   {:resource-paths ["dev" "config/dev"]
                     :dependencies   [[binaryage/devtools "1.0.5"]]}
             :local {:resource-paths ["config/local"]}
             :prod  {:resource-paths ["config/prod"]}
             :test  {:resource-paths ["config/test"]
                     :dependencies   [[clj-http "3.12.3"]
                                      [cheshire "5.10.2"]
                                      [mvxcvi/cljstyle "0.15.0"
                                       :exclusions [org.clojure/clojure]]]}}
  :shadow-cljs {:nrepl  {:port 8777}
                :builds {:app {:target     :browser
                               :output-dir "resources/public/assets/js/compiled"
                               :asset-path "assets/js/compiled"
                               :modules    {:app {:init-fn  state-events.core/init
                                                  :preloads [devtools.preload]}}}}}
  :aliases {"check-style" ["with-profile" "+test" "run" "-m" "cljstyle.main" "check"]
            "ci"          ["do" "clean," "cloverage," "lint," "uberjar"]
            "kondo"       ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]
            "lint"        ["do" "kondo," "eastwood," "kibit"]
            "migrate"     ["run" "-m" "xiana.db.migrate"]
            "watch"       ["with-profile" "dev" "do"
                           ["shadow" "watch" "app" "browser-test" "karma-test"]]
            "release"     ["with-profile" "prod" "do"
                           ["shadow" "release" "app"]]})
