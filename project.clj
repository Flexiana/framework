(defproject com.flexiana/framework "0.4.0-rc3"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[clj-http "3.12.3"]
                 [com.flexiana/tiny-rbac "0.1.1"]
                 [crypto-password "0.3.0"]
                 [funcool/cats "2.4.2"]
                 [org.clojure/core.async "1.5.648"]
                 [org.clojure/data.json "2.4.0"]
                 [http-kit "2.5.3"]
                 [info.sunng/ring-jetty9-adapter "0.17.4"]
                 [http.async.client "1.3.1"]
                 [metosin/reitit "0.5.15"]
                 [metosin/jsonista "0.3.5"]
                 [migratus "1.3.5"]
                 [nilenso/honeysql-postgres "0.4.112"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.postgresql/postgresql "42.3.3"]
                 [clj-test-containers "0.5.0"]
                 [org.testcontainers/testcontainers "1.16.3"]
                 [piotr-yuxuan/closeable-map "0.35.0"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [yogthos/config "1.1.9"]
                 [com.taoensso/timbre "5.1.2"]]
  :source-paths ["src"]
  :target "target/%s/"
  :profiles {:dev      {:resource-paths ["config/dev"]}
             :codox    {:resource-paths ["config/dev"]
                        :plugins        [[lein-codox "0.10.7"]]
                        :jvm-opts       ["--add-opens" "java.base/java.lang=ALL-UNNAMED"]
                        :codox          {:output-path "docs/new/"
                                         :themes      [:default :xiana]
                                         :namespaces  [#"framework" #"xiana"]
                                         :source-uri  "https://github.com/Flexiana/framework/blob/{git-commit}/{filepath}#L{line}"
                                         :doc-files   ["doc/Getting-Started.md", "doc/How-To.md", "doc/Development-Guide.md"]}}
             :local    {:resource-paths ["config/local"]}
             :prod     {:resource-paths ["config/prod"]}
             :cljstyle {:dependencies []}
             :test     {:source-paths   ["test"]
                        :resource-paths ["config/test"]
                        :dependencies   [[lambdaisland/kaocha "1.63.998"]
                                         [lambdaisland/kaocha-cloverage "1.0.75"]
                                         [mvxcvi/cljstyle "0.15.0"
                                          :exclusions [org.clojure/clojure]]
                                         [clj-kondo "2021.01.20"]
                                         [nubank/matcher-combinators "3.3.1"]]}}

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
