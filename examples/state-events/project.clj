(defproject state-events "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :dependencies [[com.flexiana/framework "0.5.0-rc2"]
                 [metosin/jsonista "0.3.6"]
                 [tick "0.5.0-RC5"]]
  :main ^:skip-aot state-events.core
  :uberjar-name "state-events.jar"
  :source-paths ["src/backend" "src/shared"]
  :clean-targets ^{:protect false} ["resources/public/assets/js/compiled" "target"]
  :profiles {:dev   {:source-paths ["dev"]
                     :resource-paths ["config/dev"]
                     :dependencies   [[binaryage/devtools "1.0.6"]
                                      [org.clojure/tools.namespace "1.3.0"]]
                     :plugins [[lein-shadow "0.4.0"]]}
             :frontend {:source-paths ["src/frontend"]
                        :dependencies [[thheller/shadow-cljs "2.19.0"]
                                       [cljs-ajax "0.8.4"]
                                       [re-frame "1.2.0"]]}
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
  :aliases {"check-style" ["with-profile" "test" "run" "-m" "cljstyle.main" "check"]
            "ci"          ["do" "clean," "cloverage," "lint," "uberjar"]
            "kondo"       ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]
            "lint"        ["do" "kondo," "eastwood," "kibit"]
            "test"        ["with-profile" "test" "test"]
            "migrate"     ["run" "-m" "xiana.db.migrate"]
            "watch"       ["with-profile" "dev,frontend" "do"
                           ["shadow" "watch" "app" "browser-test" "karma-test"]]
            "release"     ["with-profile" "prod,frontend" "do"
                           ["shadow" "release" "app"]]})
