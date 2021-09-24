(defproject com.flexiana/framework "0.2.4"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[clj-http "3.12.0"]
                 [com.draines/postal "2.0.4"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [com.stuartsierra/component "1.0.0"]
                 [crypto-password "0.2.1"]
                 [duct/server.http.jetty "0.2.1"]
                 [funcool/cats "2.4.1"]
                 [funcool/cuerdas "RELEASE"]
                 [garden "1.3.10"]
                 [hiccup "1.0.5"]
                 [hickory "0.7.1"]
                 [honeysql "1.0.444"]
                 [metosin/reitit "0.5.15"]
                 [migratus "1.3.3"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [nubank/matcher-combinators "3.1.4"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.postgresql/postgresql "42.2.2"]
                 [potemkin "0.4.5"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [yogthos/config "1.1.7"]]
  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
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
