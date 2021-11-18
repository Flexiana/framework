(ns state-events.interceptors.event-process-test
  (:require
    [clojure.test :refer :all]
    [state-events.interceptors.event-process :as pr]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(deftest event-interceptor-handles-one-event
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        state {:request      {:body   {:id       resource-uuid
                                       :action   :create
                                       :resource :persons}
                              :method :put}
               :session-data {:users/id user-id}}
        event (-> state
                  pr/->event
                  xiana/extract
                  :request-data
                  :event)
        expectation {:payload     {:id       resource-uuid
                                   :resource "persons"},
                     :resource-id resource-uuid
                     :resource    :persons
                     :action      :create,
                     :creator     #uuid"54749a36-8305-4adb-a69c-d447ea19de45"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [event]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))

(deftest event-interceptor-aggregates-events
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:body   {:id       resource-uuid
                                  :action   :create
                                  :resource :persons}
                         :method :put}
                        {:body   {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:body   {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :city     "Fiorenze"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :phone    "+123465789"}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event)) states)
        expectation {:action      :modify
                     :creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :payload     {:city       "Fiorenze"
                                   :email      "Doe@john.it"
                                   :first-name "John"
                                   :last-name  "Doe"
                                   :phone      "+123465789"
                                   :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                   :resource   "persons"}
                     :resource    :persons
                     :resource-id "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] events))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))

(deftest event-interceptor-one-undo-events
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:body   {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:body   {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:body   {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:body   {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event)) states)
        expectation {:action      :modify
                     :creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :payload     {:first-name "John"
                                   :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                   :resource   "persons"}
                     :resource    :persons
                     :resource-id "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] events))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))

(deftest event-interceptor-two-undo-events
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:body   {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:body   {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:body   {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}
                        {:body   {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:body   {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event)) states)
        expectation {:action      :modify
                     :creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :payload     {:first-name "John"
                                   :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                   :resource   "persons"}
                     :resource    :persons
                     :resource-id "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] events))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))

(deftest event-interceptor-modify-after-undo
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:body   {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:body   {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:body   {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:body   {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}]

        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event)) states)
        expectation {:action      :modify
                     :creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :payload     {:email      "Doe@john.it"
                                   :first-name "John"
                                   :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                   :resource   "persons"}
                     :resource    :persons
                     :resource-id "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] events))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))

(deftest event-interceptor-delete
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:body   {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:body   {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:body   {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:body   {:action   :delete
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]

        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event)) states)
        expectation {:action      :delete
                     :creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :payload     {}
                     :resource    :persons
                     :resource-id "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] events))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))

(deftest event-interceptor-delete-and-modify
  (let [resource-uuid (UUID/fromString "68849768-fc9f-4602-814e-8b6cbeedd4b3")
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:body   {:id       resource-uuid
                                  :action   :create
                                  :resource :persons}
                         :method :put}
                        {:body   {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :city     "Fiorenze"}
                         :method :post}
                        {:body   {:action   :delete
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:body   {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:body   {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :phone    "+123465789"}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event)) states)
        expectation {:action      :modify
                     :creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :payload     {:id        "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                   :last-name "Doe"
                                   :phone     "+123465789"
                                   :resource  "persons"}
                     :resource    :persons
                     :resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] events))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :modified-at))))))
