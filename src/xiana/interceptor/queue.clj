(ns xiana.interceptor.queue
  "Interceptor executor.
  Collects and executes interceptors and the given action in between."
  (:require
    [taoensso.timbre :as log]))

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
    (remove (set except-interceptors)
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
           (assoc state :error e)))))

(defn looper
  [state interceptors action]
  (loop [state        state
         interceptors interceptors
         backwards    '()
         action       action
         direction    :enter]
    (let [direction-error?    (= :error direction)
          direction-enter?    (= :enter direction)
          exception           (:error state)]

      (cond
        ;; just got an exception, executing all remaining interceptors backwards
        (and exception (not direction-error?))
        (recur state
               (if direction-enter? backwards interceptors)
               '()
               action
               :error)

        ;; executes current direction (:enter, :leave or :error)
        (seq interceptors)
        (let [direction (if (and direction-error? (not exception))
                          :leave ; error was "handled", executing remaining interceptors (:leave direction)
                          direction)
              act       (-> interceptors
                            first
                            (get direction identity)
                            action->try)
              state     (act state)
              next-interceptors (if (and (:error state) (= :leave direction))
                                  interceptors
                                  (rest interceptors))]
          (log/debug (format "%s | %s | err?: %s | exception %s"
                             direction
                             (-> interceptors first :name)
                             direction-error?
                             exception))
          (recur state
                 next-interceptors
                 (when direction-enter? (conj backwards (first interceptors)))
                 action
                 direction))

        direction-enter?
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
