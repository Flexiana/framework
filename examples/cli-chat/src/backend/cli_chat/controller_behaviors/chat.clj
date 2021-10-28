(ns cli-chat.controller-behaviors.chat
  (:require
    [clojure.string :as str]
    [org.httpkit.server :as server]))

(defn send-multi-line
  [ch msg]
  (doseq [m (str/split-lines msg)]
    (server/send! ch m false)))

(defn broadcast
  [{:keys [ch income-msg channels]}]
  (let [username (get-in @channels [ch :user :users/name])]
    (doseq [c (remove #(#{ch} (key %)) @channels)]
      (send-multi-line (key c) (str username ": " income-msg)))))

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
  [{ch           :ch
    channels     :channels
    session-data :session-data}]
  (let [username (gen-username)
        session (if (-> session-data :user :users/name)
                  session-data
                  (assoc-in session-data [:user :users/name] username))]
    (swap! channels assoc ch session)
    (send-multi-line ch (welcome-message username))))

(defn help
  [{:keys [ch income-msg]}]
  (server/send! ch "Help message!"))

(defn login
  [{:keys [ch income-msg]}]
  (server/send! ch "init login seq!"))
