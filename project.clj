(defproject framework "0.1.0"
  :description "Framework"
  :url "https://github.com/Flexiana/framework"
  :license {:name "FIXME" :url "FIXME"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [integrant "0.8.0"]
                 [aero "1.1.6"]]
  :target "target/%s/"
  :resource-paths ["resources" "config"]
  :main framework.components.core
  )
