(ns framework.interceptor.queue
  (:require
   [xiana.core :as xiana]))

(defn- -execute-fn
  "Execute the interceptor function (enter or leave)."
  [state interceptor-fn]
  (let [container-fn (or interceptor-fn xiana/ok)]
    (container-fn state)))

;; TODO: simplify this function
(defn- -execute
  "Execute interceptors functions (the enter/leave procedures)
  passing the state as its arguments."
  [state interceptors action]
  ;; finish the interceptors enter stack? if so, call the action
  (if (empty? interceptors)
    (action state)
    ;; otherwise fetch the enter-fn, leave-fn from the top interceptor
    (let [{:keys [enter leave error]} (first interceptors)]
      ;; wrap the interceptor stack execution to our flow (monad/either)
      (try
        (xiana/flow-> state
                      ;; execute enter interceptor function
                      (-execute-fn enter)
                      ;; recursive call, implicit: execute all enter-fn first
                      (-execute (rest interceptors) action)
                      ;; execute leave interceptors function
                      (-execute-fn leave))
        ;; otherwise return the state container with the response
        ;; wrapped (failure: /monad/either/left)
        (catch Exception e
          ;; if we have a error function defined call it
          (if error
            (error state)
            ;; otherwise return the monad/either/right state container
            (xiana/error
             (assoc state
                    :response
                    {:status 500 :body (Throwable->map e)}))))))))

(defn- -concat
  "Concatenate routes interceptors with the defaults ones,
  or override it if its type isn't a map."
  [interceptors default-interceptors]
  (if (map? interceptors)
    (let [around-interceptors (get interceptors :around)
          inside-interceptors (get interceptors :inside)]
      (concat around-interceptors
              default-interceptors
              inside-interceptors))
    ;; override
    (or interceptors default-interceptors)))

(defn execute
  "Execute the interceptors queue and invoke the
  action procedure between its enter-leave stacks."
  [state default-interceptors]
  (let [interceptors (-concat
                      (get-in state [:request-data :interceptors])
                      default-interceptors)
        action (get-in state [:request-data :action])]
    ;; execute the interceptors queue calling the action
    ;; between its enter/leave stacks
    (-execute state interceptors action)))
