(ns framework.interceptor.queue
  (:require
   [xiana.core :as xiana]))

;; Question: how to handle the error state from the interceptors?
(defn- -execute
  "Execute interceptors functions (the enter/leave procedures)
  passing the state as its arguments."
  [state interceptors action]
  (let [enter (mapv :enter interceptors)
        leave (reverse (mapv :leave interceptors))
        queue (apply concat [enter action leave])]
    (try
      ;; execute interceptors/action monad wrapped functions queue
      (xiana/apply-flow-> state queue)
      ;; handle exceptions
      (catch Exception e
        (xiana/error
         (assoc state
                :response
                {:status 500 :body (Throwable->map e)}))))))

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
