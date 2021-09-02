(defproject conway "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[com.github.seancorfield/honeysql "2.0.783"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/clojure "1.10.3"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [clj-http "3.12.3"]
                 [integrant "0.8.0"]
                 [migratus "1.3.5"]
                 [environ "1.2.0"]
                 [ring "1.9.4"]]
  :main ^:skip-aot conway.core
  :source-paths ["source"]
  :uberjar-name "conway.jar"
  :profiles {:uberjar {:aot :all}})
