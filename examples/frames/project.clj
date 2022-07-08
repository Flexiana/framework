(defproject frames "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :dependencies [[com.flexiana/framework "0.5.0-rc3"]]
  :plugins [[lein-shadow "0.4.0"]]
  :main ^:skip-aot frames.core
  :uberjar-name "frames.jar"
  :source-paths ["src/backend/" "src/frontend"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :profiles {:dev   {:resource-paths ["config/dev"]
                     :dependencies   [[binaryage/devtools "1.0.5"]]}
             :local {:resource-paths ["config/local" "resources"]}
             :prod  {:resource-paths ["config/prod" "resources"]}
             :test  {:resource-paths ["config/test" "resources"]
                     :dependencies   [[mvxcvi/cljstyle "0.15.0"
                                       :exclusions [org.clojure/clojure]]]}}
  :shadow-cljs {:nrepl  {:port 8777}

                :builds {:app {:target     :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules    {:app {:init-fn  donor.core/init
                                                  :preloads [devtools.preload]}}}}}
  :aliases {"check-style" ["with-profile" "+test" "run" "-m" "cljstyle.main" "check"]
            "ci"          ["do" "clean," "cloverage," "lint," "uberjar"]
            "kondo"       ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]
            "lint"        ["do" "kondo," "eastwood," "kibit"]
            "watch"       ["with-profile" "dev" "do"
                           ["shadow" "watch" "app" "browser-test" "karma-test"]]
            "release"     ["with-profile" "prod" "do"
                           ["shadow" "release" "app"]]})
