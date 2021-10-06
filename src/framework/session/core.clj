(ns framework.session.core
  (:require
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

;; define session protocol
(defprotocol Session
  ;; fetch an element (no side effect)
  (fetch [_ k])
  ;; fetch all elements (no side effect)
  (dump [_])
  ;; add an element (side effect)
  (add! [_ k v])
  ;; delete an element (side effect)
  (delete! [_ k])
  ;; erase all elements (side effect)
  (erase! [_]))

(defn init-in-memory
  "Initialize session in memory."
  ([] (init-in-memory (atom {})))
  ([m]
   ;; implement the Session protocol
   (reify Session
     ;; fetch session key:element
     (fetch [_ k] (get @m k))
     ;; fetch all elements (no side effect)
     (dump [_] @m)
     ;; add session key:element
     (add!
       [_ k v]
       (let [k (or k (UUID/randomUUID))]
         (swap! m assoc k v)))
     ;; delete session key:element
     (delete! [_ k] (swap! m dissoc k))
     ;; erase session
     (erase! [_] (reset! m {})))))

(defn interceptor
  "Session interceptor."
  ([] (interceptor (init-in-memory)))
  ([session-instance]
   {:enter
    (fn [{request :request :as state}]
      (let [session-id (try (UUID/fromString
                              (get-in request [:headers :session-id]))
                            (catch Exception _ nil))
            session-data (when session-id
                           (fetch session-instance
                                  session-id))]

        (if session-id
          ;; associate session in state
          (xiana/ok (assoc state :session-data session-data))
          ;; new session
          (xiana/error {:response {:status 403 :body "Session expired"}}))))
    :leave
    (fn [state]
      (let [session-id (get-in state [:session-data :session-id])]
        ;; dissociate session data
        (add! session-instance
              session-id
              (dissoc (:session-data state) :new-session))
        ;; associate the session id
        (xiana/ok
          (assoc-in state
                    [:response :headers "Session-id"]
                    (str session-id)))))}))
