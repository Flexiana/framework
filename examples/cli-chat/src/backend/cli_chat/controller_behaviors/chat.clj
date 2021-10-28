(ns cli-chat.controller-behaviors.chat
  (:require
    [clojure.string :as str]
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
    {reply :reply}       :response-data
    :as                  state}]
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
  [{{ch           :ch
     channels     :channels
     session-data :session-data} :request-data
    :as                          state}]
  (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                :reply    "Help message"})))

(defn login
  [{{ch           :ch
     channels     :channels
     session-data :session-data} :request-data
    :as                          state}]
  (xiana/ok (update state :response-data merge {:reply-fn send-multi-line
                                                :reply    "Init login seq!"})))

(defn broadcast
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [username (get-in @channels [ch :user :users/name])]
    (xiana/ok (update state :response-data merge {:reply-fn broadcast-to-others
                                                  :reply    (str username ": " income-msg)}))))
