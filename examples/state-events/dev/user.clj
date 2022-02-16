(ns user
  (:gen-class)
  (:require
    [clojure.tools.namespace.repl :refer [refresh-all]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [shadow.cljs.devtools.api :as shadow.api]
    [shadow.cljs.devtools.server :as shadow.server]
    [state :refer [dev-sys]]
    [state-events.core :refer [->system app-cfg]]))

(def dev-app-config
  app-cfg)

(defn- stop-dev-system
  []
  (when (:webserver @dev-sys) (do (.close @dev-sys)
                                  (refresh-all)))
  (reset! dev-sys (closeable-map {})))

(defn start-dev-system
  []
  (stop-dev-system)
  (shadow.server/start!)
  (shadow.api/watch :app)
  (reset! dev-sys (->system dev-app-config)))

(comment
  (start-dev-system))
