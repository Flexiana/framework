(ns xiana.interceptor.queue
  "Interceptor executor.
  Collects and executes interceptors and the given action in between.")

(defn- -concat
  "Concatenate routes interceptors with the defaults ones,
  or override it if its type isn't a map."
  [{except-interceptors :except
    around-interceptors :around
    inside-interceptors :inside
    :as                 interceptors}
   default-interceptors]
  (if (map? interceptors)
    ;; get around/inside interceptors
    (remove (into #{} except-interceptors)
            (concat around-interceptors
                    default-interceptors
                    inside-interceptors))
    ;; else override
    (or interceptors default-interceptors)))

(defn action->try
  [action]
  (fn [state]
    (try (action state)
         (catch Exception e
           (assoc state :exception e)))))

(defn looper
  [state interceptors action]
  (loop [state        state
         interceptors interceptors
         backwards    '()
         action       action
         direction    :enter]
    (let [error?    (= :error direction)
          enter?    (= :enter direction)
          exception (:exception state)]
      (cond
        (and exception (not error?))
        (recur state
               (if enter? backwards interceptors)
               '()
               action
               :error)

        (seq interceptors)
        (let [direction (if (and error? (not exception))
                          :leave ; error was "handled"
                          direction)
              act       (-> interceptors
                            first
                            (get direction identity)
                            action->try)]
          (recur (act state)
                 (rest interceptors)
                 (when enter? (conj backwards (first interceptors)))
                 action
                 direction))

        enter?
        (recur ((action->try action) state)
               backwards
               '()
               identity
               :leave)
        :else state))))

(defn execute
  "Execute the interceptors queue and invoke the
  action procedure between its enter-leave stacks."
  [state default-interceptors]
  (let [interceptors (-concat
                       (get-in state [:request-data :interceptors])
                       default-interceptors)
        action       (action->try (get-in state [:request-data :action] identity))]
    ;; execute the interceptors queue calling the action
    ;; between its enter/leave stacks
    (looper state interceptors action)))
