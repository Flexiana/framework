(defproject com.flexiana/framework "0.4.0-rc3"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[clj-http "3.12.0"]
                 [clj-test-containers "0.5.0"]
                 [com.draines/postal "2.0.4"]
                 [com.flexiana/tiny-rbac "0.1.1"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [com.wsscode/pathom3 "2021.10.20-alpha"]
                 [com.wsscode/tailwind-garden "2021.04.09"]
                 [crypto-password "0.2.1"]
                 [funcool/cats "2.4.1"]
                 [funcool/cuerdas "RELEASE"]
                 [garden "1.3.10"]
                 [hiccup "1.0.5"]
                 [hickory "0.7.1"]
                 [honeysql "1.0.444"]
                 [http-kit "2.5.3"]
                 [http.async.client "1.3.1"]
                 [metosin/reitit "0.5.15"]
                 [migratus "1.3.3"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [nubank/matcher-combinators "3.1.4"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.postgresql/postgresql "42.2.2"]
                 [org.testcontainers/testcontainers "1.16.2"]
                 [piotr-yuxuan/closeable-map "0.35.0"]
                 [potemkin "0.4.5"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [yogthos/config "1.1.9"]
                 [com.taoensso/timbre "5.1.2"]]
  :source-paths ["src"]
  :target "target/%s/"
  :profiles {:dev      {:resource-paths         ["config/dev"]}
             :codox {:resource-paths         ["config/dev"]
                     :plugins [[lein-codox "0.10.7"]]
                     :jvm-opts ["--add-opens" "java.base/java.lang=ALL-UNNAMED"]
                     :codox {:output-path "docs/new/"
                             :themes [:default :xiana]
                             :namespaces [#"framework" #"xiana"]
                             :source-uri "https://github.com/Flexiana/framework/blob/{git-commit}/{filepath}#L{line}"
                             :doc-files ["doc/Getting-Started.md", "doc/How-To.md", "doc/Development-Guide.md"]}}
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
