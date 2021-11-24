(ns user
  (:gen-class)
  (:require
    [clojure.tools.logging :refer [*tx-agent-levels*]]
    [clojure.tools.namespace.repl :refer [refresh disable-unload!]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [shadow.cljs.devtools.api :as shadow.api]
    [shadow.cljs.devtools.server :as shadow.server]
    [state-events.core :refer [->system app-cfg]]))

(alter-var-root #'*tx-agent-levels* conj :debug :trace)

(disable-unload!)

(defonce dev-sys (atom (closeable-map {})))

(def dev-app-config
  app-cfg)

(defn- stop-dev-system
  []
  (when (:webserver @dev-sys) (.close @dev-sys))
  (reset! dev-sys (closeable-map {})))

(defn- start-dev-system
  []
  (stop-dev-system)
  (refresh)
  (shadow.server/start!)
  (shadow.api/watch :app)
  (reset! dev-sys (->system dev-app-config)))

(comment
  (start-dev-system))
