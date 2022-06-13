(ns xiana.interceptor.queue-test
  (:require
    [clojure.test :refer :all]
    [xiana.interceptor.queue :as queue]))

(def A-interceptor
  {:enter (fn [state] (assoc state :enter "A-enter"))
   :leave (fn [state] (assoc state :leave "A-leave"))})

(def B-interceptor
  {:enter (fn [state] (assoc state :enter "B-enter"))
   :leave (fn [state] (assoc state :leave "B-leave"))})

(def C-interceptor
  {:enter (fn [state] (assoc state :enter "C-enter"))
   :leave (fn [state] (assoc state :leave "C-leave"))})

;; Exception
(def D-interceptor
  {:enter (fn [_state] (throw (Exception. "enter-exception")))})

(defn- err-handler [state] (update state :errors conj :err))

(def E-interceptor
  {:name :E-interceptor
   :enter (fn [_state] (throw (Exception. "enter-exception")))
   :leave (fn [_state] (throw (Exception. "leave-exception")))
   :error err-handler})

(def F-interceptor
  {:enter (fn [state] (assoc state :enter "F-enter"))
   :leave (fn [state] (assoc state :leave "F-leave"))})

(def default-interceptors
  "Default interceptors."
  [A-interceptor])

(def inside-interceptors
  {:inside [B-interceptor]})

(def around-interceptors
  {:around [C-interceptor]})

(def both-interceptors
  {:inside [B-interceptor]
   :around [C-interceptor]})

(def except-interceptors
  {:except [A-interceptor]})

(def override-interceptors
  [F-interceptor])

(def ok-action
  "Auxiliary ok container function."
  #(assoc % :response {:status 200, :body "ok"}))

(def throw-action
  "Auxiliary error container function."
  (fn [_] (throw (ex-info "Some error" {:status 500 :body "Exception thrown"}))))

(defn make-state
  "Return a simple state request data."
  [action interceptors]
  {:request-data
   {:action       action
    :interceptors interceptors}})

(deftest queue-simple-ok-execution
  (let [state    (make-state ok-action [])
        response (-> state
                     (queue/execute [])
                     :response)
        expected {:status 200, :body "ok"}]
    (is (= response expected))))

(deftest queue-simple-error-execution
  (let [error-action #(assoc % :response {:status 500 :body "Internal Server error"})
        state        (make-state error-action [])
        response     (-> state
                         (queue/execute [])
                         :response)
        expected     {:status 500 :body "Internal Server error"}]
    (is (= response expected))))

(deftest queue-doesnt-handle-exception
  (let [state (-> (make-state (fn [_] (/ 1 0)) [])
                  (queue/execute []))]
    (is (nil? (:response state)))
    (is (= "Divide by zero" (-> state  :error Throwable->map :cause)))))

(deftest queue-error-handled-in-same-interceptor
  (let [state (make-state
                identity
                [{:leave (fn [_]
                           (throw (ex-info "it should be handled in `:error`" {})))
                  :error (fn [state]
                           (-> state
                               (dissoc :error)
                               (assoc :response
                                      {:status 200, :body "ok"})))}])
        response (queue/execute state [])
        expected {:status 200, :body "ok"}]
    (is (= expected (:response response)))))

(deftest queue-error-handled-in-first-interceptor
  (let [response            {:status 200, :body "fixed"}
        recover-interceptor {:error (fn [state] (assoc state :response response))}
        state               (make-state throw-action [recover-interceptor])
        result              (-> state
                                (queue/execute [])
                                :response)]
    (is (= response result))))

(deftest queue-default-interceptors-execution
  (let [state    (make-state ok-action nil)
        result   (queue/execute state default-interceptors)
        response (:response result)
        expected {:status 200 :body "ok"}
        enter    (:enter result)
        leave    (:leave result)]
    (is (and
          (= enter "A-enter")
          (= leave "A-leave")
          (= response expected)))))

(deftest queue-interceptor-exception-default-execution
  (let [state (make-state ok-action [D-interceptor])
        res   (queue/execute state [])
        cause (-> res :error Throwable->map :cause)]
    (is (nil? (:response state)))
    (is (= "enter-exception" cause))))

(deftest queue-interceptor-one-error-handler
  (testing "error path with one :error handler on the chain; error isn't handled"
    (let [state (make-state ok-action [E-interceptor])
          res   (queue/execute state [])]
      (is (= :err (-> res :errors first))))))

(deftest queue-interceptors-error->error->
  (testing "error path with two :error handlers on the chain; error isn't handled"
    (let [state (make-state ok-action [{:error err-handler :name :e-int0}
                                       E-interceptor])
          res   (queue/execute state [])]
      (is (= '(:err :err) (-> res :errors))))))

(deftest queue-interceptors-error->leave->
  (testing "error path with two an:error handlers on the chain; error is handled"
    (let [interceptors [A-interceptor {:error #(dissoc % :error)} E-interceptor]
          state        (make-state ok-action interceptors)
          {:keys [errors error leave]} (queue/execute state [])]
      (is (= :err (first errors)))
      (is (nil? error))
      (is (= "A-leave" leave)))))

(deftest queue-inside-interceptors-execution
  (let [state      (make-state ok-action inside-interceptors)
        result     (queue/execute state default-interceptors)
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    (is (and
          (= last-enter "B-enter")
          (= last-leave "A-leave")
          (= response expected)))))

(deftest queue-around-interceptors-execution
  (let [state      (make-state ok-action around-interceptors)
        result     (queue/execute state default-interceptors)
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    (is (and
          (= last-enter "A-enter")
          (= last-leave "C-leave")
          (= response expected)))))

(deftest queue-both-interceptors-execution
  (let [state      (make-state ok-action both-interceptors)
        result     (queue/execute state default-interceptors)
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    (is (and
          (= last-enter "B-enter")
          (= last-leave "C-leave")
          (= response expected)))))

(deftest queue-override-interceptors-execution
  (let [state      (make-state ok-action override-interceptors)
        result     (queue/execute state default-interceptors)
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    (is (and
          (= last-enter "F-enter")
          (= last-leave "F-leave")
          (= response expected)))))

(deftest queue-both-interceptors-execution
  (let [state      (make-state ok-action except-interceptors)
        result     (queue/execute state default-interceptors)
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    (is (nil? last-enter))
    (is (nil? last-leave))
    (is (= expected response))))
