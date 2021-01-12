(defproject framework "0.1.0"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.stuartsierra/component "1.0.0"]
                 [yogthos/config "1.1.7"]]
  :target "target/%s/"
  :profiles {:dev   {:resource-paths ["config/dev"]}
             :local {:resource-paths ["config/local"]}
             :prod  {:resource-paths ["config/prod"]}}
  :main framework.components.core
  )
