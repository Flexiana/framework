(ns xiana.interceptor.queue-test
  (:require
    [clojure.test :refer :all]
    [xiana.core :as xiana]
    [xiana.interceptor.queue :as queue]))

(def A-interceptor
  {:enter (fn [state] (xiana/ok (assoc state :enter "A-enter")))
   :leave (fn [state] (xiana/ok (assoc state :leave "A-leave")))})

(def B-interceptor
  {:enter (fn [state] (xiana/ok (assoc state :enter "B-enter")))
   :leave (fn [state] (xiana/ok (assoc state :leave "B-leave")))})

(def C-interceptor
  {:enter (fn [state] (xiana/ok (assoc state :enter "C-enter")))
   :leave (fn [state] (xiana/ok (assoc state :leave "C-leave")))})

;; Exception
(def D-interceptor
  {:enter (fn [state] (throw (Exception. "enter-exception")))})

;; Error/Exception
(def E-interceptor
  {:enter (fn [state] (throw (Exception. "enter-exception")))
   :leave (fn [state] (throw (Exception. "leave-exception")))
   :error (fn [state] (xiana/ok (assoc state :error "Error")))})

(def F-interceptor
  {:enter (fn [state] (xiana/ok (assoc state :enter "F-enter")))
   :leave (fn [state] (xiana/ok (assoc state :leave "F-leave")))})

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
  #(xiana/ok
     (assoc % :response {:status 200, :body "ok"})))

(def error-action
  "Auxiliary error container function."
  #(xiana/error
     (assoc % :response {:status 500 :body "Internal Server error"})))

(def throw-action
  "Auxiliary error container function."
  (fn [_] (throw (ex-info "Some error" {:status 500 :body "Exception thrown"}))))

(defn make-state
  "Return a simple state request data."
  [action interceptors]
  {:request-data
   {:action action
    :interceptors interceptors}})

;; test a simple interceptor queue ok execution
(deftest queue-simple-ok-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action [])
        ;; get response using a simple micro flow
        response (-> state
                     (queue/execute [])
                     (xiana/extract)
                     (:response))
        expected {:status 200, :body "ok"}]
    ;; verify if response is equal to the expected
    (is (= response expected))))

;; test a simple interceptor queue execution
(deftest queue-simple-error-execution
  ;; construct a simple request data state
  (let [state (make-state error-action [])
        ;; get response using a simple micro flow
        response (-> state
                     (queue/execute [])
                     (xiana/extract)
                     (:response))
        expected {:status 500 :body "Internal Server error"}]
    ;; verify if response is equal to the expected
    (is (= response expected))))

(deftest queue-simple-thrown-execution
  ;; construct a simple request data state
  (let [state (make-state throw-action [])
        ;; get response using a simple micro flow
        response (-> state
                     (queue/execute [])
                     (xiana/extract)
                     (:response))
        expected {:body   "Exception thrown"
                  :status 500}]
    ;; verify if response is equal to the expected
    (is (= response expected))))

(deftest queue-simple-thrown-div0
  ;; construct a simple request data state
  (let [state (make-state (fn [_] (/ 1 0)) [])
        ;; get response using a simple micro flow
        response (-> state
                     (queue/execute [])
                     (xiana/extract)
                     (:response))]
    ;; verify if response is equal to the expected
    (is (= 500 (:status response)))
    (is (= "Divide by zero" (get-in response [:body :cause])))))

;; test default interceptors queue execution
(deftest queue-default-interceptors-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action nil)
        ;; get response using a simple micro flow
        result (-> state
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        enter    (:enter result)
        leave    (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= enter "A-enter")
          (= leave "A-leave")
          (= response expected)))))

;; test a simple interceptor error queue execution
(deftest queue-interceptor-exception-default-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action [D-interceptor])
        ;; get error response using a simple micro flow
        cause (-> state
                  (queue/execute [])
                  (xiana/extract)
                  (:response)
                  (:body)
                  (:cause))
        expected "enter-exception"]
    ;; verify if cause is equal to the expected
    (is (= cause expected))))

;; test a simple interceptor error queue execution
(deftest queue-interceptor-error-exception-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action [E-interceptor])
        ;; get error response using a simple micro flow
        error (-> state
                  (queue/execute [])
                  (xiana/extract)
                  (:error))
        expected "Error"]
    ;; verify if error is equal to the expected
    (is (= error expected))))

;; test inside interceptors queue execution
(deftest queue-inside-interceptors-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action inside-interceptors)
        ;; get response using a simple micro flow
        result (-> state
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "B-enter")
          (= last-leave "A-leave")
          (= response expected)))))

;; test around interceptors queue execution
(deftest queue-around-interceptors-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action around-interceptors)
        ;; get response using a simple micro flow
        result (-> state
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "A-enter")
          (= last-leave "C-leave")
          (= response expected)))))

;; test inside/around interceptors queue execution
(deftest queue-both-interceptors-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action both-interceptors)
        ;; get response using a simple micro flow
        result (-> state
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "B-enter")
          (= last-leave "C-leave")
          (= response expected)))))

;; test override interceptors queue execution
(deftest queue-override-interceptors-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action override-interceptors)
        ;; get response using a simple micro flow
        result (-> state
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response   (:response result)
        expected   {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    ;; verify if response is equal to the expected
    (is (and
          (= last-enter "F-enter")
          (= last-leave "F-leave")
          (= response expected)))))

(deftest queue-both-interceptors-execution
  ;; construct a simple request data state
  (let [state (make-state ok-action except-interceptors)
        ;; get response using a simple micro flow
        result (-> state
                   (queue/execute default-interceptors)
                   (xiana/extract))
        response (:response result)
        expected {:status 200 :body "ok"}
        last-enter (:enter result)
        last-leave (:leave result)]
    (is (nil? last-enter))
    (is (nil? last-leave))
    (is (= expected response))))
