(ns cli-chat.controller-behaviors.chat
  (:require
    [cli-chat.models.users :as user]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [framework.auth.hash :as auth]
    [framework.db.sql :as sql]
    [org.httpkit.server :as server]
    [xiana.core :as xiana]))

(defn send-multi-line
  ([ch reply]
   (doseq [m (str/split-lines reply)]
     (server/send! ch (str m "\n"))))
  ([{{req-ch :ch}    :request-data
     {res-ch :ch
      reply  :reply} :response-data}]
   (send-multi-line (or res-ch req-ch) reply)))

(defn broadcast-to-others
  [{{ch       :ch
     channels :channels} :request-data
    {reply :reply}       :response-data}]
  (doseq [c (remove #(#{ch} (key %)) @channels)]
    (server/send! (key c) reply)))

(defn broadcast
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [username (get-in @channels [ch :user :users/name])]
    (xiana/ok (update state :response-data merge {:reply-fn broadcast-to-others
                                                  :reply    (str username ": " income-msg)}))))

(defn gen-username []
  (let [id (apply str (take 4 (repeatedly #(char (+ (rand 26) 65)))))]
    (str "guest_" id)))

(defn welcome-message
  [username]
  (format "HELLo tHERE, %s

  type '/sign-up' to register
  '/login username' to log in,
   get help with '/help'
   " username))

(defn welcome
  [{{ch       :ch
     channels :channels} :request-data
    session-data         :session-data
    :as                  state}]
  (let [username (gen-username)
        session (if (-> session-data :user :users/name)
                  session-data
                  (assoc-in session-data [:user :users/name] username))]
    (swap! channels assoc ch session)
    (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                  :reply    (welcome-message username)}))))

(defn help
  [state]
  (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                :reply    "Help message"})))

(defn update-user!
  [{{ch       :ch
     channels :channels} :request-data
    {db-data :db-data}   :response-data
    :as                  state}]
  (let [logable-user (-> (first db-data)
                         (dissoc :users/passwd))]
    (log/info "update user: " logable-user)
    (swap! channels update ch assoc :user logable-user)
    (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                  :reply    (str logable-user)}))))

(defn valid-password?
  [{{income-msg :income-msg} :request-data
    {db-data :db-data}       :response-data
    :as                      state}]
  (let [msg (str/split income-msg #"\s")
        [_ _ password] msg
        encrypted (-> db-data
                      first
                      :users/passwd)]
    (try (auth/check state password encrypted)
         (catch Exception _ false))))

(defn password-side
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (let [msg (str/split income-msg #"\s")
        [_ user-name password] msg]
    (if (and user-name password (valid-password? state))
      (do
        (update-user! state)
        (xiana/ok
          (update state :response-data merge {:reply-fn send-multi-line
                                              :reply    "Successful login"})))
      (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                    :reply    "Invalid username or password"})))))

(defn login
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (let [msg (str/split income-msg #"\s")
        [_ user-name password] msg]
    (if (and user-name password)
      (xiana/ok (assoc state
                  :side-effect password-side
                  :query (user/get-user user-name)))
      (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                    :reply    (str "Usage:\n'/login username password'")})))))

(defn sign-up
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (let [[_ user-name passwd] (str/split income-msg #"\s")
        exists (not-empty (sql/execute (-> state :deps :db :datasource) (user/get-user user-name)))]
    (cond (and user-name passwd (not exists))
          (xiana/ok (assoc state
                      :query {:insert-into :users
                              :values      [{:name user-name :passwd (auth/make state passwd) :role "member"}]}
                      :side-effect update-user!))
          exists
          (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                        :reply    "Username already registered"}))
          :else
          (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                        :reply    "Usage:\n/sing-up username password"})))))

(defn me
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [user (get-in @channels [ch :user])
        username (get-in @channels [ch :user :users/name])
        msg (str/split income-msg #"\s")]
    (if (second msg)
      (xiana/ok (update state :response-data merge {:reply-fn broadcast-to-others
                                                    :reply    (str/replace-first income-msg #"\/me" username)}))
      (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                    :reply    (str user)})))))

(defn user->ch
  [channels name]
  (->> @channels
       (filter (fn [[_ v]] (= name (get-in v [:user :users/name]))))
       first))

(defn to
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [[_ to] (str/split income-msg #"\s")
        from (get-in @channels [ch :user :users/name])
        user-ch (some->> to
                         (user->ch channels)
                         key)
        message (try (-> income-msg
                         (str/replace-first to "")
                         (str/replace-first #"\/to" (format ">>%s>>" from)))
                     (catch Exception _ nil))]
    (xiana/ok (cond (and user-ch message)
                    (update state :response-data merge {:ch       user-ch
                                                        :reply-fn send-multi-line
                                                        :reply    message})
                    message
                    (update state :response-data merge {:reply-fn send-multi-line
                                                        :reply    (format "User %s not logged in" to)})
                    :else
                    (update state :response-data merge {:reply-fn send-multi-line
                                                        :reply    "Usage:\n /to username message"})))))
