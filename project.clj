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
                 [funcool/cats "2.4.1"]]
  :target "target/%s/"
  :profiles {:dev   {:resource-paths ["config/dev"]}
             :local {:resource-paths ["config/local"]}
             :prod  {:resource-paths ["config/prod"]}}
  :main framework.components.core
  )
