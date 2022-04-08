(ns cli-chat.controller-behaviors.chat
  (:require
    [cli-chat.models.users :as user]
    [cli-chat.views.chat :as views]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [xiana.core :as xiana]
    [xiana.db :as sql]
    [xiana.hash :as auth]))

(defn gen-username []
  (let [id (apply str (take 4 (repeatedly #(char (+ (rand 26) 65)))))]
    (str "guest_" id)))

(defn welcome-message
  [username]
  (format "HELLo tHERE, %s

   type '/help' for get help
   " username))

(defn welcome
  [{{ch       :ch
     channels :channels} :request-data
    session-data         :session-data
    :as                  state}]
  (let [username (gen-username)
        session (if (-> session-data :users/name)
                  session-data
                  (assoc-in session-data [:users/name] username))]
    (swap! channels assoc ch session)
    (update state :response-data merge {:reply-fn views/send-multi-line
                                        :reply    (welcome-message username)})))

(def help-messages
  {"/login"   "Usage:\n /login username password"
   "/sign-up" "Usage:\n /sing-up username password"
   "/to"      "Usage:\n /to username message"
   "/me"      "Usage:\n /me the get user info\n /me message to send personalized message"})

(defn help
  [state]
  (update state :response-data merge {:reply-fn views/send-multi-line
                                      :reply    (str/join "\n" help-messages)}))

(defn update-user!
  [{{ch       :ch
     channels :channels} :request-data
    {db-data :db-data}   :response-data
    :as                  state}]
  (let [loggable-user (-> (first db-data)
                          (dissoc :users/passwd))]
    (log/info "update user: " loggable-user)
    (swap! channels update ch assoc :user loggable-user)
    (update state :response-data merge {:reply-fn views/send-multi-line
                                        :reply    (str loggable-user)})))

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
        (update state :response-data merge {:reply-fn views/send-multi-line
                                            :reply    "Successful login"}))
      (update state :response-data merge {:reply-fn views/send-multi-line}
                                      :reply    "Invalid username or password"))))

(defn login
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (let [msg (str/split income-msg #"\s")
        [_ user-name password] msg]
    (if (and user-name password)
      (assoc
        state
        :side-effect password-side
        :query (user/get-user user-name))
      (update state :response-data merge {:reply-fn views/send-multi-line
                                          :reply    (help-messages "/login")}))))

(defn sign-up
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (let [[_ user-name passwd] (str/split income-msg #"\s")
        exists (not-empty (sql/execute (-> state :deps :db :datasource) (user/get-user user-name)))]
    (cond (and user-name passwd (not exists))
          (assoc
            state
            :query {:insert-into :users
                    :values      [{:name user-name :passwd (auth/make state passwd) :role "member"}]}
            :side-effect update-user!)
          exists
          (update state :response-data merge {:reply-fn views/send-multi-line
                                              :reply    "Username already registered"})
          :else
          (update state :response-data merge {:reply-fn views/send-multi-line
                                              :reply    (help-messages "/sign-up")}))))

(defn me
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [user (get @channels ch)
        username (get-in @channels [ch :users/name])
        msg (str/split income-msg #"\s")]
    (if (second msg)
      (update state :response-data merge {:reply-fn views/broadcast-to-others
                                          :reply    (str/replace-first income-msg #"\/me" username)})
      (update state :response-data merge {:reply-fn views/send-multi-line
                                          :reply    (str user)}))))

(defn user->ch
  [channels name]
  (->> @channels
       (filter (fn [[_ v]] (= name (get v :users/name))))
       first))

(defn to
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [[_ to] (str/split income-msg #"\s")
        from (get-in @channels [ch :users/name])
        user-ch (some->> to
                         (user->ch channels)
                         key)
        message (try (-> income-msg
                         (str/replace-first to "")
                         (str/replace-first #"\/to" (format ">>%s>>" from)))
                     (catch Exception _ nil))]
    (cond (and user-ch message)
          (update state :response-data merge {:ch       user-ch
                                              :reply-fn views/send-multi-line
                                              :reply    message})
          message
          (update state :response-data merge {:reply-fn views/send-multi-line
                                              :reply    (format "User %s not logged in" to)})
          :else
          (update state :response-data merge {:reply-fn views/send-multi-line}
                                    :reply    (help-messages "/to")))))
