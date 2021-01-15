(defproject handlers "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :min-lein-version "2.0.0"
            :dependencies [[org.clojure/clojure "1.10.1"]
                           [com.flexiana/framework "0.1.0"]]
            :plugins []
            :main ^:skip-aot components
            :uberjar-name "handlers.jar"
            :source-paths   ["app" "components"]
            :profiles {:dev   {:resource-paths ["config/dev"]}
                       :local {:resource-paths ["config/local"]}
                       :prod  {:resource-paths ["config/prod"]}}
            :aliases {"ci"    ["do" "clean," "cloverage," "lint," "uberjar"]
                      "kondo" ["run" "-m" "clj-kondo.main" "--lint" "src" "test"]
                      "lint"  ["do" "kondo," "eastwood," "kibit"]})
  
