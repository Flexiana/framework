(ns cli-chat.controller-behaviors.chat
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [framework.auth.hash :as auth]
    [org.httpkit.server :as server]
    [xiana.core :as xiana]))

(defn send-multi-line
  ([ch reply]
   (doseq [m (str/split-lines reply)]
     (server/send! ch (str m "\n"))))
  ([{{ch :ch}       :request-data
     {reply :reply} :response-data}]
   (send-multi-line ch reply)))

(defn broadcast-to-others
  [{{ch       :ch
     channels :channels} :request-data
    {reply :reply}       :response-data}]
  (doseq [c (remove #(#{ch} (key %)) @channels)]
    (server/send! (key c) reply)))

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

(defn login
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [msg (str/split income-msg #"\s")]
    (if-not (str/blank? (second msg))
      (let [user-name (second msg)]
        (swap! channels assoc-in [ch :login] {:status    :in-progress
                                              :user-name user-name})
        (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                      :reply    (str "Type '/password \"password\"' to log in as " user-name)})))
      (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                    :reply    (str "Usage:\n'/login username'")})))))

(defn update-user
  [{{ch       :ch
     channels :channels} :request-data
    {db-data :db-data}   :response-data
    :as                  state}]
  (log/info "update user: " db-data)
  (swap! channels update ch assoc :user (dissoc (first db-data) :users/passwd))
  (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                :reply    (str db-data)})))

(defn sign-up
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (let [[_ user-name passwd] (str/split income-msg #"\s")]
    (if (and user-name passwd)
      (xiana/ok (assoc state
                       :query {:insert-into :users
                               :values      [{:name user-name :passwd (auth/make state passwd)}]}
                       :side-effect update-user))
      (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                    :reply    (str "Usage:\n'/sing-up username password'")})))))

(defn valid-password?
  [{{income-msg :income-msg} :request-data
    {db-data :db-data}       :response-data
    :as                      state}]
  (let [msg (str/split income-msg #"\s")
        password (second msg)
        encrypted (-> db-data
                      first
                      :users/passwd)]
    (try (auth/check state password encrypted)
         (catch Exception _ false))))

(defn password-side
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    {db-data :db-data}       :response-data
    :as                      state}]
  (log/info "db-data" db-data)
  (log/info (get @channels ch))
  (let [msg (str/split income-msg #"\s")]
    (if-not (or (str/blank? (second msg))
                (not= :in-progress (get-in @channels [ch :login :status])))
      (if (valid-password? state)
        (do
          (update-user state)
          (xiana/ok
            (update state :response-data merge {:reply-fn send-multi-line
                                                :reply    "Successful login"})))
        (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                      :reply    "Invalid username/password"})))
      (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                    :reply    "Init login sequence with '/login username' command,\n Usage:\n'/password password'"})))))

(defn get-user
  [user-name]
  {:select [:*]
   :from   [:users]
   :where  [:= :name user-name]})

(defn password
  [{{ch       :ch
     channels :channels} :request-data
    :as                  state}]
  (let [user-name (get-in @channels [ch :login :user-name])]
    (xiana/ok (assoc state
                     :side-effect password-side
                     :query (get-user user-name)))))

(defn broadcast
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [username (get-in @channels [ch :user :users/name])]
    (xiana/ok (update state :response-data merge {:reply-fn broadcast-to-others
                                                  :reply    (str username ": " income-msg)}))))

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
