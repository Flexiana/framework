(ns xiana.hotreload
  (:require
   [clojure.core.async :refer [go-loop]]
   [ns-tracker.core :refer [ns-tracker]]))

;;; reloader function from ring.middleware.reload

(defn- reloader [dirs retry?]
  (let [modified-namespaces (ns-tracker dirs)
        load-queue (java.util.concurrent.LinkedBlockingDeque.)]
    (fn []
      (locking load-queue
        (doseq [ns-sym (reverse (modified-namespaces))]
          (.push load-queue ns-sym))
        (loop []
          (when-let [ns-sym (.peek load-queue)]
            (if retry?
              (do (require ns-sym :reload) (.remove load-queue))
              (do (.remove load-queue) (require ns-sym :reload)))
            (recur)))))))

(defn hotreload
  "Function to hotreload the system. Add it after server-start at configuration phase \"->system\".
  (-> (config/config app-cfg)
      ...
      xiana.webserver/start
      xiana.hotreload/hotreload)
  If a :xiana/hotreload config key is provided it needs:
  :restart-fn \"the function to restart the system\" if none 'user/start-dev-system
  :tracker {:dirs \"the directories vector list to search for changes\" :reload-compile-errors? \"true\"}."
  [cfg]
  (let [{:keys [restart-fn tracker]} (:xiana/hotreload cfg {:restart-fn 'user/start-dev-system})
        dirs (:dirs tracker ["src"])
        retry? (:reload-compile-errors? tracker true)
        track-fn (ns-tracker dirs)
        reload! (reloader dirs retry?)
        restart-fn (resolve restart-fn)]
    (go-loop []
      (if (track-fn)
        (do
          (reload!)
          (restart-fn))
        (recur)))
    cfg))
