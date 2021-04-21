(ns framework.components.runner
  (:require
    [xiana.core :as xiana]))

(defn execute
  [state action]
  (if action (action state)
      (xiana/ok state)))

(defn run
  ([state action]
   (run state [] action))
  ([state interceptors action]
   (if (empty? interceptors)
     (action state)
     (let [{:keys [enter leave error]} (first interceptors)]
       (try (xiana/flow-> state
                          (execute enter)
                          (run (rest interceptors) action)
                          (execute leave))
            (catch Exception e
              (if error
                (error state)
                (xiana/error (assoc state :response {:status 500
                                                     :body   e})))))))))
