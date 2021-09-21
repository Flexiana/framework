(ns framework.interceptor.queue
  (:require
    [xiana.core :as xiana]))


(defn- interceptor->fn
  "Parse the interceptor function 'side' (:enter/:leave) to
  a lambda function that uses the try-catch approach to
  handle the interceptor exception if occurs."
  [side interceptor]
  (when-let [f (side interceptor)]
    (fn [state]
      (try
        (f state)
        (catch Exception e
          (let [f (or (:error interceptor) xiana/error)]
            (f (-> state (assoc :response
                                {:status 500
                                 :body   (Throwable->map e)})))))))))


(defn- -execute
  "Execute interceptors functions (the enter/leave procedures)
  passing the state as its arguments."
  [state interceptors action]
  (let [enter-fns (mapv #(interceptor->fn :enter %) interceptors)
        leave-fns (mapv #(interceptor->fn :leave %) interceptors)
        queue-fns (apply concat [enter-fns action (reverse leave-fns)])]
    ;; apply flow: execute the queue of interceptors functions
    (xiana/apply-flow-> state (remove nil? queue-fns))))


(defn- -concat
  "Concatenate routes interceptors with the defaults ones,
  or override it if its type isn't a map."
  [interceptors default-interceptors]
  (if (map? interceptors)
    ;; get around/inside interceptors
    (let [around-interceptors (get interceptors :around)
          inside-interceptors (get interceptors :inside)]
      (concat around-interceptors
              default-interceptors
              inside-interceptors))
    ;; else override
    (or interceptors default-interceptors)))


(defn execute
  "Execute the interceptors queue and invoke the
  action procedure between its enter-leave stacks."
  [state default-interceptors]
  (let [interceptors (-concat
                       (get-in state [:request-data :interceptors])
                       default-interceptors)
        action (vector (get-in state [:request-data :action]))]
    ;; execute the interceptors queue calling the action
    ;; between its enter/leave stacks
    (-execute state interceptors action)))
