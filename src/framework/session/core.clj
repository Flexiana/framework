(ns framework.session.core
  "Xiana's session management"
  (:require
    [clojure.string :as string]
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

(defn on-enter
  [protected-path
   excluded-resource
   {{headers      :headers
     cookies      :cookies
     query-params :query-params
     uri          :uri} :request
    :as                 state}]
  (if (and (string/starts-with? uri protected-path)
           (not= uri (str protected-path excluded-resource)))
    (try (let [session-backend (-> state :deps :session-backend)
               session-id (UUID/fromString (or (some->> headers
                                                        :session-id)
                                               (some->> cookies
                                                        :session-id
                                                        :value)
                                               (some->> query-params
                                                        :SESSIONID)))
               session-data (or (fetch session-backend session-id)
                                (throw (ex-message "Missing session data")))]
           (xiana/ok (assoc state :session-data (assoc session-data :session-id session-id))))
         (catch Exception _
           (xiana/error
             (assoc state :response {:status 401
                                     :body   "Invalid or missing session"}))))
    (xiana/ok state)))

(defn interceptor
  "On enter allows a resource to be served when
      * it is not protected
   or
      * the user-provided `session-id` exists in the server's session store.
   If the session exists in the session store, it's copies it to the (-> state :session-data),
   else responds with {:status 401
                       :body   \"Invalid or missing session\"}
   
   On leave, it updates the session storage from (-> state :session-data)"
  [protected-path excluded-resource]
  {:enter (partial on-enter protected-path excluded-resource)
   :leave (fn [state]
            (let [session-backend (-> state :deps :session-backend)
                  session-id (get-in state [:session-data :session-id])]
              (add! session-backend
                    session-id
                    (:session-data state))
              ;; associate the session id
              (xiana/ok
                (assoc-in state
                          [:response :headers "Session-id"]
                          (str session-id)))))})
