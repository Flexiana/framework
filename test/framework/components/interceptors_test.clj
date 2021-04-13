(ns framework.components.interceptors-test
  (:require
    [clojure.test :refer :all]
    [framework.components.interceptors :as interceptors]
    [framework.components.session.backend :as session-backend]))

(def session-id #uuid"21f0d6e6-3782-465a-b903-ca84f6f581a0")

(deftest store-fetch-session-via-interceptor
  (let [session-bcknd (assoc-in {} [:deps :session-backend] (session-backend/init-in-memory-session))
        state (-> (assoc-in session-bcknd [:session-data :user] {:id 1 :name "john"})
                  (assoc-in [:session-data :session-id] session-id))
        response (:right ((:leave interceptors/session-interceptor) state))
        session (get-in response [:response :headers "Session-id"])
        request (assoc-in session-bcknd [:request :headers :session-id] session)
        new-state ((:enter interceptors/session-interceptor) request)]
    (is (= {:user       {:id 1, :name "john"}
            :session-id #uuid"21f0d6e6-3782-465a-b903-ca84f6f581a0"}
           (-> new-state :right :session-data)))))
