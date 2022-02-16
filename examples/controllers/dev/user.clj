(ns user
  (:gen-class)
  (:require
    [clojure.tools.namespace.repl :refer [refresh-all]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]
    [state :refer [dev-sys]]
    [core :refer [->system app-cfg]]))

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
  (reset! dev-sys (->system dev-app-config)))

(comment
  (start-dev-system))
