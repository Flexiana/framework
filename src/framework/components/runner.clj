(ns framework.components.runner)

(defn run
  ([state action]
   (run state [] action))
  ([state interceptors action]
   (if (empty? interceptors)
     (action state)
     (let [{:keys [enter leave error]} (first interceptors)]
       (try (cond-> state
              enter enter
              :default (run (rest interceptors) action)
              leave leave)
            (catch Exception e (error state)))))))
