(ns unit.state-events.event-process-test
  (:require
    [clojure.test :refer :all]
    [state-events.interceptors.event-process :as pr]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn- <-db
  "Mimics database store and query"
  [event]
  (into {} (map (fn [[k v]]
                  [(keyword "events" (name k)) v])
                event)))

(deftest event-interceptor-handles-one-event
  (let [resource-uuid (UUID/fromString "68849768-fc9f-4602-814e-8b6cbeedd4b3")
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        state {:request      {:params {:id       (str resource-uuid)
                                       :action   :create
                                       :resource :persons}
                              :method :put}
               :session-data {:users/id user-id}}
        event (-> state
                  pr/->event
                  xiana/extract
                  :request-data
                  :event
                  <-db)
        expectation {:events/payload     {:id       (str resource-uuid)
                                          :resource "persons"},
                     :events/resource-id resource-uuid
                     :events/resource    "persons"
                     :events/action      "create",
                     :events/creator     user-id}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil [event]]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-aggregates-events
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:id       resource-uuid
                                  :action   :create
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}
                        {:params {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :city     "Fiorenze"}
                         :method :post}
                        {:params {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :phone    "+123465789"}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (mapv (fn [state] (-> state
                                     pr/->event
                                     xiana/extract
                                     :request-data
                                     :event
                                     <-db)) states)
        expectation {:events/action      "modify"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:city       "Fiorenze"
                                          :email      "Doe@john.it"
                                          :first-name "John"
                                          :last-name  "Doe"
                                          :phone      "+123465789"
                                          :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource   "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-one-undo-events
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event
                                    <-db)) states)
        expectation {:events/action      "undo"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:first-name "John"
                                          :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource   "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-two-undo-events
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}
                        {:params {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:params {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event
                                    <-db)) states)
        expectation {:events/action      "undo"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:first-name "John"
                                          :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource   "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-modify-after-undo
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:params {:action   :modify
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
                                    :event
                                    <-db)) states)
        expectation {:events/action      "modify"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:email      "Doe@john.it"
                                          :first-name "John"
                                          :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource   "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-clean
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :clean
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]

        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event
                                    <-db)) states)
        expectation {:events/action      "clean"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:id "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-clean-and-modify
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:id       resource-uuid
                                  :action   :create
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :email    "Doe@john.it"}
                         :method :post}
                        {:params {:action   :modify
                                  :id       resource-uuid
                                  :resource :persons
                                  :city     "Fiorenze"}
                         :method :post}
                        {:params {:action   :clean
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :modify
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
                                    :event
                                    <-db)) states)
        expectation {:events/action      "modify"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:id        "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :last-name "Doe"
                                          :phone     "+123465789"
                                          :resource  "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-one-undo-and-redo
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:params {:action   :redo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event
                                    <-db)) states)
        expectation {:events/action      "redo"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:first-name "John"
                                          :last-name  "Doe"
                                          :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource   "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))

(deftest event-interceptor-clean-and-undo
  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        user-id (UUID/fromString "54749a36-8305-4adb-a69c-d447ea19de45")
        event-requests [{:params {:action   :create
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :put}
                        {:params {:action     :modify
                                  :id         resource-uuid
                                  :resource   :persons
                                  :first-name "John"}
                         :method :post}
                        {:params {:action    :modify
                                  :id        resource-uuid
                                  :resource  :persons
                                  :last-name "Doe"}
                         :method :post}
                        {:params {:action   :clean
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}
                        {:params {:action   :undo
                                  :id       resource-uuid
                                  :resource :persons}
                         :method :post}]
        states (map (fn [request] {:request      request
                                   :session-data {:users/id user-id}}) event-requests)
        events (map (fn [state] (-> state
                                    pr/->event
                                    xiana/extract
                                    :request-data
                                    :event
                                    <-db)) states)
        expectation {:events/action      "undo"
                     :events/creator     #uuid "54749a36-8305-4adb-a69c-d447ea19de45"
                     :events/payload     {:first-name "John"
                                          :last-name  "Doe"
                                          :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                          :resource   "persons"}
                     :events/resource    "persons"
                     :events/resource-id #uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"}]
    (is (= expectation
           (-> (pr/->aggregate (assoc-in {} [:response-data :db-data] [nil events]))
               xiana/extract
               :response-data
               :event-aggregate
               (dissoc :events/modified-at))))))
