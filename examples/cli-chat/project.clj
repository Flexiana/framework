(defproject cli-chat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.flexiana/framework "0.3.4"]
                 [thheller/shadow-cljs "2.11.26"]
                 [migratus "1.3.5"]
                 [clj-http "3.12.1"]
                 [reagent "0.10.0"]
                 [re-frame "1.1.2"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]]
  :plugins [[lein-shadow "0.3.1"]
            [migratus-lein "0.7.3"]]
  :main ^:skip-aot cli-chat.core
  :uberjar-name "cli-chat.jar"
  :source-paths ["src/backend" "src/frontend" "src/shared"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :profiles {:dev   {:resource-paths ["config/dev"]
                     :dependencies   [[binaryage/devtools "1.0.3"]]}
             :local {:resource-paths ["config/local"]}
             :prod  {:resource-paths ["config/prod"]}
             :test  {:resource-paths ["config/test"]
                     :dependencies   [[kerodon "0.9.1"]
                                      [mvxcvi/cljstyle "0.15.0"
                                       :exclusions [org.clojure/clojure]]]}}
  :shadow-cljs {:nrepl  {:port 8777}
                :builds {:app {:target     :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules    {:app {:init-fn cli-chat.core/init
                                                  :preloads [devtools.preload]}}}}}
  :aliases {"check-style" ["with-profile" "+test" "run" "-m" "cljstyle.main" "check"]
            "ci"      ["do" "clean," "cloverage," "lint," "uberjar"]
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
                             :subname     "//localhost:5433/cli_chat"
                             :user        "postgres"
                             :password    "postgres"}})
