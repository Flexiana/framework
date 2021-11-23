(ns user
  (:gen-class)
  (:require
    [clojure.tools.logging :refer [*tx-agent-levels*]]
    [shadow.cljs.devtools.api :as shadow.api]
    [shadow.cljs.devtools.server :as shadow.server]
    [state-events.core :refer [->system app-cfg]]))

(alter-var-root #'*tx-agent-levels* conj :debug :trace)

(defonce dev-sys (atom {}))

(def dev-app-config
  app-cfg)

(defn- stop-dev-system
  []
  (when (:webserver @dev-sys) (.close @dev-sys))
  (reset! dev-sys {}))

(defn- start-dev-system
  []
  (stop-dev-system)
  (shadow.server/start!)
  (shadow.api/watch :app)
  (reset! dev-sys (->system dev-app-config)))

(comment
  (start-dev-system))
