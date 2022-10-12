(ns xiana.interceptor-test
  (:require
    [clojure.test :refer :all]
    [xiana.db :as db]
    [xiana.interceptor :as interceptor]
    [xiana.session :as session]))

(def sample-session-id
  "Sample session id."
  "21f0d6e6-3782-465a-b903-ca84f6f581a0")

(def sample-authorization
  "Sample authorization data."
  "auth")

(def sample-request
  "Sample request example."
  {:uri            "/"
   ;; method example: GET
   :request-method :get
   ;; header example with session id and authorization
   :headers        {:session-id    sample-session-id
                    :authorization sample-authorization}})

;; minimal request
(def simple-request
  "Simple/minimal request example."
  {:uri "/" :request-method :get})

(def sample-state
  "State with the sample request."
  {:request sample-request
   :deps    (session/init-backend {})})

(def simple-state
  "State with the simple/minimal request."
  {:request simple-request})

(def sample-query
  {:query {:select [:role]
           :from   [:users]
           :where  [:= :users/username "admin"]}})

(def ok-fn
  "Ok response function."
  #(assoc % :response {:status 200, :body "ok"}))

;; auxiliary function
(defn fetch-execute
  "Fetch and execute the interceptor function"
  [state interceptor branch]
  (apply (branch interceptor) (list state)))

(deftest log-interceptor-execution
  (let [interceptor interceptor/log
        state {}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)]
    ;; verify log execution
    (is (and (= enter state)
             (= leave state)))))

(deftest side-effect-execution
  (let [state {:request     {:uri "/"}
               :side-effect ok-fn}
        ;; bind response using the simulated micro/flow
        response (-> state
                     (fetch-execute interceptor/side-effect :leave)
                     (:response))
        ;; expected response
        expected {:status 200, :body "ok"}]
    ;; verify if the response is equal to the expected
    (is (= response expected))))

(deftest view-execution
  (let [response (fetch-execute {:request :view}
                                interceptor/view
                                :leave)
        expected {:request :view}]
    (is (= response expected))))

(deftest params-execution
  (let [state {:request {}}
        request (fetch-execute state interceptor/params :enter)
        expected {:request {:form-params  {},
                            :params       {},
                            :query-params {}}}]
    ;; expected request value?
    (is (= request expected))))

;; (deftest contains-sql-query-result
;;  (let [response-data (-> sample-query
;;                          (fetch-execute interceptor/db-access :leave)
;;                          (:response-data)) ; interceptor execution
;;        ;; expected value
;;        expected {:db-data [#:users{:role "admin"}]}]
;;    ;; expected response data value?
;;    (is (= expected response-data))))

(deftest contains-sql-empty-result
  (let [result (fetch-execute {} db/db-access :leave)
        ;; expected value
        expected {:response-data {:db-data nil}}]
    ;; expected response data value?
    (is (= expected result))))

(deftest msg-interceptor-execution
  (let [interceptor (interceptor/message "")
        state {}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)]
    ;; verify msg execution
    (is (and (= enter state)
             (= leave state)))))

(deftest contains-muuntaja-interceptor
  (let [interceptor (interceptor/muuntaja)]
    (is (seq interceptor))))

(deftest prunes-get-request-bodies
  (testing "GET request"
    (let [state {:request sample-request}]
      (is (= state (fetch-execute state interceptor/prune-get-request-bodies :enter)))
      (is (= state (fetch-execute (assoc-in state [:request :body] "TEST BODY") interceptor/prune-get-request-bodies :enter)))
      (is (= state (fetch-execute (assoc-in state [:request :body-params] {:param "value"}) interceptor/prune-get-request-bodies :enter)))))
  (testing "POST request"
    (let [request (-> sample-request
                      (assoc :body {:param1 1 :param2 2})
                      (assoc :request-method :post))
          state {:request request}]
      (is (= state (fetch-execute state interceptor/prune-get-request-bodies :enter))))))
