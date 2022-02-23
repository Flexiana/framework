(ns framework.session.core
  "Xiana's session management"
  (:require
    [clojure.string :as string]
    [clojure.string :as str]
    [framework.db.core :as db]
    [honeysql.core :as sql]
    [jsonista.core :as json]
    [next.jdbc.result-set :refer [as-kebab-maps]]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)
    (org.postgresql.util
      PGobject)))

(defmulti where->filter
  (fn [& w] (first w)))

(defmethod where->filter :=
  [_ [a b]]
  #(= (get (val %) a) b))

(defmethod where->filter :>
  [_ [a b]]
  #(> (get (val %) a) b))

(defmethod where->filter :<
  [_ [a b]]
  #(< (get (val %) a) b))

(defmethod where->filter :>=
  [_ [a b]]
  #(>= (get (val %) a) b))

(defmethod where->filter :<=
  [_ [a b]]
  #(<= (get (val %) a) b))

(defmethod where->filter :<>
  [_ [a b]]
  #(not= (get (val %) a) b))

(defmethod where->filter :!=
  [_ [a b]]
  #(not= (get (val %) a) b))

(defmethod where->filter :between
  [_ [a b c]]
  #(<= b (get (val %) a) c))

(defn log [x] (prn x) x)

(defmethod where->filter :like
  [_ [a ^String b]]
  #(re-matches
     (re-pattern (string/replace b #"%" ".*"))
     (get (val %) a "")))

(defmethod where->filter :in
  [_ [a b]]
  #((set b) (get (val %) a)))

(defmethod where->filter :not
  [_ [w]]
  #(not ((where->filter (first w) (rest w)) %)))

(defmethod where->filter :and
  [_ ws]
  (fn [entry]
    (let [fn-s (map (fn [w]
                      (fn [v]
                        ((where->filter (first w) (rest w)) v))) ws)]
      (every? some? (map (fn [f] (or (f entry) nil)) fn-s)))))

(def -any? (complement not-any?))

(defmethod where->filter :or
  [_ ws]
  (fn [entry]
    (let [fn-s (map (fn [w]
                      (fn [v]
                        ((where->filter (first w) (rest w)) v))) ws)]
      (-any? some? (map (fn [f] (or (f entry) nil)) fn-s)))))

;; define session protocol
(defprotocol Session
  ;; fetch an element (no side effect)
  (fetch [_ k])
  ;; fetch all elements (no side effect)
  (dump [_])
  ;; filter with where statement
  (dump-where [_ where-clause])
  ;; add an element (side effect)
  (add! [_ k v])
  ;; delete an element (side effect)
  (delete! [_ k])
  ;; erase all elements (side effect)
  (erase! [_]))

(def mapper (json/object-mapper {:decode-key-fn keyword}))
(def ->json json/write-value-as-string)
(def <-json #(json/read-value % mapper))

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^PGobject v]
  (let [type (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (some-> value
              <-json
              (with-meta {:pgtype type}))
      value)))

(defn un-objectify
  [table data]
  (let [{session-data (keyword (name table) "session_data")
         session-id   (keyword (name table) "session_id")
         modified-at  (keyword (name table) "modified_at")} data]
    {session-id (some-> session-data
                        <-pgobject
                        (assoc :modified-at modified-at))}))

(defn ->session-data [table rs]
  (when-let [session-data (first rs)]
    (let [[_ data] (first (un-objectify table session-data))]
      data)))

(defn connect
  [{backend-config :xiana/session-backend :as cfg}]
  (let [ds-config {:xiana/postgresql backend-config
                   :xiana/jdbc-opts  {:builder-fn as-kebab-maps}}
        connection (cond (every? backend-config [:port :dbname :host :dbtype :user :password]) (db/connect ds-config)
                         (get-in cfg [:db :datasource]) cfg
                         :else (db/connect {:xiana/postgresql
                                            (assoc (merge (:xiana/postgresql cfg) backend-config)
                                                   :xiana/jdbc-opts {:builder-fn as-kebab-maps})}))]
    (get-in connection [:db :datasource])))

(defn- init-in-db
  "Initialize persistent database session storage."
  [{backend-config :xiana/session-backend :as cfg}]
  (let [ds (connect cfg)
        table (:session-table-name backend-config :sessions)
        get-all {:select [:*]
                 :from   [table]}
        get-one (fn [k] {:select [:session_data :modified_at]
                         :from   [table]
                         :where  [:= :session_id k]})
        where-selector (fn ws [[op k & value]]
                         (cond (coll? k) (into [op (ws k) (ws (first value))])
                               (not (keyword? op)) [op k value]
                               :else (let [f (if (namespace k) (str/join "/" [(namespace k) (name k)]) (name k))
                                           v (if (coll? (first value)) [(map str (first value))]
                                                 (map str value))]
                                       (prn op k value)
                                       (prn (coll? (first value)))
                                       (into [op (sql/raw (format "session_data ->> '%s' " f))] v))))

        insert-session (fn [k v] {:insert-into table
                                  :values      [{:session_id   k
                                                 :session_data (->pgobject v)}]
                                  :upsert      {:on-conflict   [:session_id]
                                                :do-update-set [:session_data :modified-at]}})
        erase-session-store {:truncate table}
        delete-session (fn [k] {:delete-from table
                                :where       [:= :session_id k]})
        unpack (partial un-objectify table)]
    (assoc cfg
           :session-backend
           ;; implement the Session protocol
           (reify Session
             ;; fetch session key:element
             (fetch [_ k] (->session-data table (db/execute ds (get-one k))))
             ;; fetch all elements (no side effect)
             (dump [_] (into {} (map unpack (db/execute ds get-all))))
             ;; fetching with applied filter
             (dump-where [_ where] (into {} (map unpack (db/execute ds {:select [:*]
                                                                        :from   [table]
                                                                        :where  (where-selector where)}))))
             ;; add session key:element
             (add!
               [_ k v]
               (let [k (or k (UUID/randomUUID))]
                 (when v (first (map unpack (db/execute ds (insert-session k v)))))))
             ;; delete session key:element
             (delete! [_ k] (first (map unpack (db/execute ds (delete-session k)))))
             ;; erase session
             (erase! [_] (db/execute ds erase-session-store))))))

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
            ;; fetching with applied filter
            (dump-where [_ [op & where]]
              (filter (where->filter op where) @m))
            ;; add session key:element
            (add!
              [_ k v]
              (let [k (or k (UUID/randomUUID))]
                (swap! m assoc k v)))
            ;; delete session key:element
            (delete! [_ k] (swap! m dissoc k))
            ;; erase session
            (erase! [_] (reset! m {}))))))

(defn ->session-id
  [{{headers      :headers
     cookies      :cookies
     query-params :query-params} :request}]
  (UUID/fromString (or (some->> headers
                                :session-id)
                       (some->> cookies
                                :session-id
                                :value)
                       (some->> query-params
                                :SESSIONID))))

(defn fetch-session
  [state]
  (let [session-backend (-> state :deps :session-backend)
        session-id (->session-id state)
        session-data (or (fetch session-backend session-id)
                         (throw (ex-message "Missing session data")))]
    (xiana/ok (assoc state :session-data (assoc session-data :session-id session-id)))))

(defn- on-enter
  [state]
  (try (fetch-session state)
       (catch Exception _
         (xiana/error
           (assoc state :response {:status 401
                                   :body   (json/write-value-as-string
                                             {:message "Invalid or missing session"})})))))

(defn- protect
  [protected-path
   excluded-resource
   {{uri :uri} :request
    :as        state}]
  (if (and (string/starts-with? uri protected-path)
           (not= uri (str protected-path excluded-resource)))
    (on-enter state)
    (xiana/ok state)))

(defn- on-leave
  [{{session-id :session-id} :session-data :as state}]
  (if session-id
    (let [session-backend (-> state :deps :session-backend)]
      (add! session-backend
            session-id
            (:session-data state))
      ;; associate the session id
      (xiana/ok
        (assoc-in state
                  [:response :headers "Session-id"]
                  (str session-id))))
    (xiana/ok state)))

(defn protected-interceptor
  "On enter allows a resource to be served when
      * it is not protected
   or
      * the user-provided `session-id` exists in the server's session store.
   If the session exists in the session store, it's copies it to the (-> state :session-data),
   else responds with {:status 401
                       :body   \"Invalid or missing session\"}
   
   On leave, it updates the session storage from (-> state :session-data)"
  [protected-path excluded-resource]
  {:enter (partial protect protected-path excluded-resource)
   :leave on-leave})

(def interceptor
  {:enter on-enter
   :leave on-leave})

(def guest-session-interceptor
  {:enter
   (fn [state]
     (try (fetch-session state)
          (catch Exception _
            (let [session-backend (-> state :deps :session-backend)
                  session-id (UUID/randomUUID)
                  user-id (UUID/randomUUID)
                  session-data {:session-id session-id
                                :users/role :guest
                                :users/id   user-id}]
              (add! session-backend session-id session-data)
              (xiana/ok (assoc state :session-data session-data))))))
   :leave on-leave})

(defn init-backend
  [{session-backend    :session-backend
    {storage :storage} :xiana/session-backend
    :as                config}]
  (cond
    session-backend config
    (= :database storage) (init-in-db config)
    :else (init-in-memory config)))

(comment
  (def ss
    (init-backend {:xiana/postgresql      {:port     5433
                                           :dbname   "frankie"
                                           :host     "localhost"
                                           :dbtype   "postgresql"
                                           :user     "flexiana"
                                           :password "dev"}
                   :xiana/migration       {:store                :database
                                           :migration-dir        "migrations"
                                           :seeds-dir            "dev_seeds"
                                           :migration-table-name "migrations"
                                           :seeds-table-name     "seeds"}
                   :xiana/auth            {:hash-algorithm :bcrypt}
                   :xiana/session-ttl     {:duration    90
                                           :unit        :minutes
                                           :alert-times [600 300 120 90 60 30 15 10 5 0]} ; in seconds
                   :xiana/session-backend {:storage            :database
                                           :session-table-name :sessions}
                   :xiana/web-client      {:force-http-completion true}
                   :xiana/web-server      {:port   3000
                                           :host   "localhost"
                                           :join?  false
                                           :async? true}
                   :xiana/uploads         {:path "resources/public/assets/attachments/"}
                   :nrepl.server/port     7888
                   :logging/timbre-config {:min-level :info
                                           :ns-filter {:allow #{"*"}}}
                   :xiana/jdbc-opts         {:builder-fn next.jdbc.result-set/as-kebab-maps}}))

  (def sb (:session-backend ss))
  (erase! sb)
  (dump sb)
  (def id (UUID/randomUUID))
  (add! sb id {:session-id id
               :index 2
               :title "arabica"})
  (dump sb)
  (dump-where sb [:= :session-id id])
  (dump-where sb [:= :session-id "0558b901-ca0b-4c2e-8e47-6fa96e666f66"])
  (dump-where sb [:not [:= :session-id "0558b901-ca0b-4c2e-8e47-6fa96e666f66"]])
  (dump-where sb [:= :title "arabica"])
  (dump-where sb [:in :index [1 2 3]])
  (dump-where sb [:between :index 1 3])
  (dump-where sb [:and
                  [:in :title ["form" "meat" "loaf" "arabica"]]
                  [:= :index 2]])
  (dump-where sb [:and
                  [:in :title ["form" "meat" "loaf" "arabica"]]
                  [:= :index 1]])
  (dump-where sb [:or
                  [:in :title ["form" "meat" "loaf"]]
                  [:= :index 2]])


  (next.jdbc/execute! ds
                      ["select * from sessions where session_data ->> 'session-id' = ?"
                       "6552a891-4139-48b5-af53-ac13d02d3ba5"]))

(defn where-selector
  [table where]
  (let [[op k & value] where
        f (if (namespace k) (str/join "/" [(namespace k) (name k)]) (name k))
        v (if (coll? (first value)) (map str (first value)) (map str value))
        w (into [op (format "session_data ->> '%s'" f)] v)]
    (sql/format {:select [:*]
                 :from   [table]
                 :where  w})))

(where-selector :sessions [:= :session-id "0558b901-ca0b-4c2e-8e47-6fa96e666f66"])

(where-selector :sessions [:in :session-id [(UUID/randomUUID) (UUID/randomUUID)]])
(where-selector :sessions [:between :index 3 4])
(where-selector :sessions [:between :users/id 3 4])
