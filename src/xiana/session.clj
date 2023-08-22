(ns xiana.session
  "Xiana's session management"
  (:require [honeysql.format :as sqlf]
            [next.jdbc.result-set :refer [as-kebab-maps]]
            [xiana.db-provisional :as dbp] 
            [taoensso.timbre :as log]
            [jsonista.core :as json])
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

(defn un-objectify
  [table data]
  (let [{session-data (keyword (name table) "session-data")
         session-id   (keyword (name table) "session-id")
         modified-at  (keyword (name table) "modified-at")} data]
    {session-id (assoc session-data :modified-at modified-at)}))

(defn- ->session-data [table rs]
  (when-let [session-data (first rs)]
    (let [[_ data] (first (un-objectify table session-data))]
      data)))

(defn validate-config-data [record]
  (every? (into {} record) [:port :dbname :host :dbtype :user :password]))

(defn connect
  [{backend-config :xiana/session-backend :as cfg}]
  (let [selected-db (:xiana/selected-db cfg)
        db-cfg (selected-db cfg)
        db-record-instance (dbp/construct selected-db db-cfg {:builder-fn as-kebab-maps} nil)
        connection (cond (validate-config-data db-record-instance) db-record-instance
                         (get-in cfg [:db :config :datasource]) (dbp/construct selected-db db-cfg
                                                                               {:builder-fn as-kebab-maps}
                                                                               nil)
                         :else (dbp/construct selected-db (merge db-cfg backend-config)
                                              {:builder-fn as-kebab-maps}
                                              nil))]
    connection))

(defn- create-sessions-table-query [] 
  ;;  {:create-table :sessions
  ;;   :with-columns [[:session_id :uuid [:not nil]]
  ;;                  [:session_data :varchar]
  ;;                  [:modified_at :timestamp]]} Trying to figure out how to support this with Honeysql
  ["CREATE TABLE sessions (session_id CHAR (36) PRIMARY KEY,
                           session_data JSON,
                           modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);"])

(defn- init-in-db
  "Initialize persistent database session storage."
  [{backend-config :xiana/session-backend :as cfg}]
  (let [conn-obj (connect cfg)
        table (:session-table-name backend-config :sessions)
        get-all {:select [:*]
                 :from   [table]}
        get-one (fn [k] {:select [:session_data :modified_at]
                         :from   [table]
                         :where  [:= :session_id k]})
        create-session (create-sessions-table-query)
        insert-session (fn [k v]
                         (log/info "== Key to be evaluated == " k)
                         (log/info "== Value to be inserted==" v)
                         (case (-> conn-obj :config :dbtype)
                           "postgresql" {:insert-into table
                                         :values      [{:session_id   k
                                                        :session_data (sqlf/value v)}]
                                         :upsert      {:on-conflict   [:session_id]
                                                       :do-update-set [:session_data :modified-at]}}
                           "mysql" {:insert-into table
                                    :values      [{:session_id   (str k)
                                                   :session_data (json/write-value-as-string v)}]
                                    :upsert      {:on-duplicate-key-update [:session_data :modified_at]}}))
        erase-session-store {:truncate table}
        delete-session (fn [k] {:delete-from table
                                :where       [:= :session_id k]})
        unpack (partial un-objectify table)]
    (assoc cfg
           :session-backend
           ;; implement the Session protocol
           (reify Session
             ;; fetch session key:element
             (fetch [_ k] (->session-data table (.execute conn-obj (get-one k))))
             ;; fetch all elements (no side effect)
             (dump [_] (into {} (map unpack (.execute conn-obj get-all))))
             ;; add session key:element
             (add!
               [_ k v]
               (let [k (or k (UUID/randomUUID))]
                 (if (dbp/table-exists? table conn-obj)
                   (do
                     (log/info "Adding data because the table exists")
                     (when v (first (map unpack (.execute conn-obj (insert-session k v))))))
                   (do
                     (log/info "Creating session because table doesn't exists in DB")
                     (.execute conn-obj create-session)))))
             ;; delete session key:element
             (delete! [_ k] (first (map unpack (.execute conn-obj (delete-session k)))))
             ;; erase session
             (erase! [_] (.execute conn-obj erase-session-store))))))

(defn- init-in-memory
  "Initialize session in memory."
  ([cfg] (init-in-memory cfg (atom {})))
  ([cfg m]
   (assoc cfg
          :session-backend
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
            (erase! [_] (reset! m {}))))))

(defn- ->session-id
  [{{headers      :headers
     cookies      :cookies
     query-params :query-params} :request}]
  (when-let [uuid (or (some->> headers
                               :session-id)
                      (some->> cookies
                               :session-id
                               :value)
                      (some->> query-params
                               :SESSIONID))]
    (and (seq uuid) (UUID/fromString uuid))))

(defn- fetch-session
  [state]
  (let [session-backend (-> state :deps :session-backend)
        session-id (->session-id state)
        session-data (or (fetch session-backend session-id)
                         (throw (ex-info "Missing session data"
                                         {:xiana/response
                                          {:body {:message "Invalid or missing session"}
                                           :status 401}})))]
    (assoc state :session-data (assoc session-data :session-id session-id))))

(defn store-session
  [{{session-id :session-id} :session-data :as state}]
  (if session-id ; TODO: write to session backend only if session-data is changed
    (let [session-backend (-> state :deps :session-backend)]
      (add! session-backend
            session-id
            (:session-data state))
      ;; associate the session id
      (assoc-in state
                [:response :headers "Session-id"]
                (str session-id)))
    state))

(def interceptor
  "Returns with 401 when the referred session is missing"
  {:name ::session-interceptor
   :enter fetch-session
   :leave store-session})

(defn add-guest-user
  [state]
  (let [session-backend (-> state :deps :session-backend)
        session-id (UUID/randomUUID)
        user-id (UUID/randomUUID)
        session-data {:session-id session-id
                      :users/role :guest
                      :users/id   user-id}]
    (add! session-backend session-id session-data)
    (dissoc (assoc state :session-data session-data) :response)))


(def guest-session-interceptor
  "Inserts a new session when no session found"
  {:name ::guest-session-interceptor
   :enter (fn [state]
            (try (fetch-session state)
                 (catch Exception _; TODO: more specific exception, it might be better to return nil when session is missing
                   (let [session-backend (-> state :deps :session-backend)
                         session-id (UUID/randomUUID)
                         user-id (UUID/randomUUID)
                         session-data {:session-id session-id
                                       :users/role :guest
                                       :users/id   user-id}]
                     (add! session-backend session-id session-data)
                     (assoc state :session-data session-data)))))
   :leave store-session})

(defn init-backend
  [{session-backend    :session-backend
    {storage :storage} :xiana/session-backend
    :as                config}]
  (cond
    session-backend config
    (= :database storage) (init-in-db config)
    :else (init-in-memory config)))
