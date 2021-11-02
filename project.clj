(defproject com.flexiana/framework "0.3.0"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [yogthos/config "1.1.7"]
                 [honeysql "1.0.444"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [clj-http "3.12.0"]
                 [com.flexiana/tiny-rbac "0.1.1"]
                 [org.postgresql/postgresql "42.2.2"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [org.clojure/data.xml "0.0.8"]
                 [com.opentable.components/otj-pg-embedded "0.13.3"]
                 [clj-test-containers "0.5.0"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [http.async.client "1.3.1"]
                 [http-kit "2.5.3"]
                 [migratus "1.3.3"]
                 [potemkin "0.4.5"]
                 [metosin/reitit "0.5.15"]
                 [funcool/cats "2.4.1"]
                 [com.draines/postal "2.0.4"]
                 [crypto-password "0.2.1"]
                 [nubank/matcher-combinators "3.1.4"]
                 [garden "1.3.10"]
                 [hickory "0.7.1"]
                 [hiccup "1.0.5"]
                 [com.wsscode/tailwind-garden "2021.04.09"]
                 [com.wsscode/pathom3 "2021.10.20-alpha"]
                 [funcool/cuerdas "RELEASE"]]
  :source-paths ["src"]
  :target "target/%s/"
  :profiles {:dev      {:resource-paths         ["config/dev"]}
             :local    {:resource-paths ["config/local"]}
             :prod     {:resource-paths ["config/prod"]}
             :cljstyle {:dependencies []}
             :test     {:source-paths   ["test"]
                        :resource-paths ["config/test"]
                        :dependencies   [[lambdaisland/kaocha "1.0.732"]
                                         [stylefruits/gniazdo "1.2.0"]
                                         [lambdaisland/kaocha-cloverage "1.0.75"]
                                         [mvxcvi/cljstyle "0.14.0"
                                          :exclusions [org.clojure/clojure]]
                                         [clj-kondo "2021.01.20"]
                                         [nubank/matcher-combinators "3.1.4"]]}}
  :aliases {"check-style" ["with-profile"
                           "+test"
                           "run"
                           "-m"
                           "cljstyle.main"
                           "check"]
            "fix-style"   ["with-profile"
                           "+test"
                           "run"
                           "-m"
                           "cljstyle.main"
                           "fix"]
            "test"        ["with-profile"
                           "+test"
                           "run"
                           "-m"
                           "kaocha.runner"
                           "--plugin" "cloverage"]
            "pre-hook"    ["do" ["check-style"] ["do" "test"]]})
