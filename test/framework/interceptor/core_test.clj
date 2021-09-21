(ns framework.interceptor.core-test
  (:require
   [xiana.core :as xiana]
   [clojure.test :refer :all]
   [framework.session.core :as session]
   [framework.interceptor.core :as interceptor])
  (:import
   (java.util UUID)))

(def sample-session-id
  "Sample session id."
  "21f0d6e6-3782-465a-b903-ca84f6f581a0")

(def sample-authorization
  "Sample authorization data."
  "auth")

(def sample-request
  "Sample request example."
  {:uri "/"
   ;; method example: GET
   :request-method :get
   ;; header example with session id and authorization
   :headers {:session-id sample-session-id
             :authorization sample-authorization}})

;; minimal request
(def simple-request
  "Simple/minimal request example."
  {:uri "/" :request-method :get})

(def sample-state
  "State with the sample request."
  {:request sample-request})

(def simple-state
  "State with the simple/minimal request."
  {:request simple-request})

(def sample-query
  {:query {:select [:role]
           :from   [:users]
           :where  [:= :users/username "admin"]}})

;; auxiliary session user id interceptor
(def session-user-id
  "Instance of the session user id interceptor."
  (interceptor/session-user-id))

;; auxiliary session user role interceptor
(def session-user-role
  "Instance of the session user role interceptor."
  (interceptor/session-user-role))

(def ok-fn
  "Ok response function."
  #(xiana/ok
    (assoc % :response {:status 200, :body "ok"})))

;; auxiliary function
(defn fetch-execute
  "Fetch and execute the interceptor function"
  [state interceptor branch]
  (->> (list state)
       (apply (branch interceptor))
       (xiana/extract)))

(deftest log-interceptor-execution
  (let [interceptor interceptor/log
        state {}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)]
    ;; verify log execution
    (is (and (= enter (xiana/ok state))
             (= leave (xiana/ok state))))))

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
  (let [state {}
        request (-> state (fetch-execute interceptor/params :enter))
        expected {:request {:form-params {},
                            :params {},
                            :query-params {}}}]
    ;; expected request value?
    (is (= request expected))))

;(deftest contains-sql-query-result
;  (let [response-data (-> sample-query
;                          (fetch-execute interceptor/db-access :leave)
;                          (:response-data)) ; interceptor execution
;        ;; expected value
;        expected {:db-data [#:users{:role "admin"}]}]
;    ;; expected response data value?
;    (is (= expected response-data))))

(deftest contains-sql-empty-result
  (let [result (fetch-execute {} interceptor/db-access :leave)
        ;; expected value
        expected {}]
    ;; expected response data value?
    (is (= result expected))))

(deftest msg-interceptor-execution
  (let [interceptor (interceptor/message "")
        state {}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)]
    ;; verify msg execution
    (is (and (= enter (xiana/ok state))
             (= leave (xiana/ok state))))))

;; test if the session-user-id handles new sessions
(deftest contains-new-session
  (let [session-data (-> {}
                         (fetch-execute session-user-id :enter)
                         (:session-data))]
    ;; verify if session-id was registered
    (is (= (:new-session session-data) true))))

(deftest persiste-session-id
  ;; compute a single interceptor semi cycle (enter-leave-enter)
  (let [enter-resp (fetch-execute sample-state session-user-id :enter)
        leave-resp (fetch-execute enter-resp session-user-id :leave)
        header     (get-in leave-resp [:response :headers])
        new-state  (assoc-in sample-state [:request :headers] header)
        last-resp  (fetch-execute new-state session-user-id :enter)]
    ;; verify if the uuid strings are equal
    (is (= (get-in last-resp [:request :headers :session-id])
           (.toString (get-in last-resp [:session-data :session-id]))))))

(deftest contains-session-user-role
  ;; compute a single interceptor semi cycle (enter-leave-enter)
  (let [sample-resp (fetch-execute sample-state session-user-role :enter)
        simple-resp (fetch-execute simple-state session-user-role :enter)]
    ;; verify if has the user and the right authorization strings
    (is (and (= (get-in sample-resp [:session-data :user :role]) :guest)
             (= (get-in sample-resp [:session-data :authorization]) "auth")))
    ;; verify if has the user and the right authorization is nil
    (is (and (= (get-in simple-resp [:session-data :user :role]) :guest)
             (nil? (get-in simple-resp [:session-data :authorization]))))))

(deftest contains-muuntaja-interceptor
  (let [interceptor (interceptor/muuntaja)]
    (is (not (empty? interceptor)))))
