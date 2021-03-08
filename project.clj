(defproject com.flexiana/framework "0.1.3"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.stuartsierra/component "1.0.0"]
                 [yogthos/config "1.1.7"]
                 [honeysql "1.0.444"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [org.postgresql/postgresql "42.2.2"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [migratus "1.3.3"]
                 [potemkin "0.4.5"]
                 [funcool/cats "2.4.1"]
                 [com.draines/postal "2.0.4"]
                 [funcool/cuerdas "2020.03.26-3"]
                 [crypto-password "0.2.1"]
                 [garden "1.3.10"]
                 [hickory "0.7.1"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-tools-deps "0.4.5"]
            [lein-git-deps "0.0.1-SNAPSHOT"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :git-dependencies [["https://github.com/wilkerlucio/tailwind-garden.git" "main"]]
  :lein-tools-deps/config {:config-files ["deps.edn"]
                           :clojure-executables ["/usr/bin/clojure" "/usr/local/bin" "/usr/local/sbin"]}

  :source-paths [".lein-git-deps/src/main" "src"]
  :target "target/%s/"
  :profiles {:dev      {:resource-paths ["config/dev"]
                        :lein-tools-deps/config {:config-files ["deps.edn"]
                                                 :clojure-executables ["/usr/bin/clojure" "/usr/local/bin" "/usr/local/sbin"]}}
             :local    {:resource-paths ["config/local"]}
             :prod     {:resource-paths ["config/prod"]}
             :cljstyle {:dependencies []}
             :test     {:source-paths ["test"]
                        :dependencies [[lambdaisland/kaocha "1.0.732"]
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
                        "kaocha.runner"]}
  :main framework.components.core)
