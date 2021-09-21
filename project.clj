(defproject com.flexiana/framework "0.2.3"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.stuartsierra/component "1.0.0"]
                 [yogthos/config "1.1.7"]
                 [honeysql "1.0.444"]
                 [clj-test-containers "0.4.0"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [clj-http "3.12.0"]
                 [org.postgresql/postgresql "42.2.2"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [org.clojure/data.xml "0.0.8"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [duct/server.http.jetty "0.2.1"]
                 [migratus "1.3.3"]
                 [potemkin "0.4.5"]
                 [metosin/reitit "0.5.12"]
                 [funcool/cats "2.4.1"]
                 [com.draines/postal "2.0.4"]
                 [com.wsscode/tailwind-garden "2021.04.09"]
                 [com.wsscode/pathom3 "2021.08.14-alpha"]
                 [com.opentable.components/otj-pg-embedded "0.7.1"]
                 [crypto-password "0.2.1"]
                 [nubank/matcher-combinators "3.1.4"]
                 [garden "1.3.10"]
                 [hickory "0.7.1"]
                 [hiccup "1.0.5"]
                 [funcool/cuerdas "RELEASE"]]
  :plugins [[lein-tools-deps "0.4.5"]]
  ;:middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :source-paths ["src"]
  :target "target/%s/"
  :profiles {:dev      {:resource-paths         ["config/dev"]
                        :lein-tools-deps/config {:config-files [:install :user :project]}}
             :local    {:resource-paths ["config/local"]}
             :prod     {:resource-paths ["config/prod"]}
             :cljstyle {:dependencies []}
             :test     {:source-paths ["test"]
                        :dependencies [[lambdaisland/kaocha "1.0.732"]
                                       [lambdaisland/kaocha-cloverage "1.0.75"]
                                       [mvxcvi/cljstyle "0.14.0"
                                        :exclusions [org.clojure/clojure]]
                                       [clj-kondo "2021.01.20"]
                                       [nubank/matcher-combinators "3.1.4"]]}}
  :aliases {"cljstyle" ["with-profile"
                        "+test"
                        "run"
                        "-m"
                        "cljstyle.main"]
            "test"     ["with-profile"
                        "+test"
                        "run"
                        "-m"
                        "kaocha.runner"
                        "--plugin" "cloverage"]}
  :main framework.components.core)
