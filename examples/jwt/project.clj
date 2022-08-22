(defproject jwt "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :dependencies [[com.flexiana/framework "0.5.0-rc3"]
                 [org.clojure/tools.cli "1.0.206"]]
  :plugins [[lein-shadow "0.4.0"]
            [lein-shell "0.5.0"]
            [migratus-lein "0.7.3"]]
  :main ^:skip-aot app.core
  :uberjar-name "frames.jar"
  :source-paths ["src/backend/"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :profiles {:dev   {:resource-paths ["config/dev"]
                     :dependencies   [[binaryage/devtools "1.0.5"]
                                      [org.clojure/tools.nrepl "0.2.13"]
                                      [nrepl/lein-nrepl "0.3.2"]]}
             :local {:resource-paths ["config/local"]}
             :prod  {:resource-paths ["config/prod"]}
             :test  {:resource-paths ["config/test"]
                     :dependencies   [[clj-http "3.12.3"]
                                      [mvxcvi/cljstyle "0.15.0"
                                       :exclusions [org.clojure/clojure]]]}}
  :aliases {"check-style" ["with-profile" "+test" "run" "-m" "cljstyle.main" "check"]
            "ci"      ["do" "clean," "cloverage," "lint," "uberjar"]
            "kondo"   ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]
            "lint"    ["do" "kondo," "eastwood," "kibit"]
            "release" ["with-profile" "prod,frontend" "do"
                       ["shadow" "release" "app"]]}
  :migratus {:store         :database
             :migration-dir "migrations"
             :db            {:classname   "com.mysql.jdbc.Driver"
                             :subprotocol "postgres"
                             :subname     "//localhost/controllers"
                             :user        "postgres"
                             :password    "postgres"}})
