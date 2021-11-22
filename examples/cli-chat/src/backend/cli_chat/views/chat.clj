(ns cli-chat.views.chat
  (:require
    [clojure.string :as str]
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
  (let [username (get-in @channels [ch :users/name])]
    (xiana/ok (update state :response-data merge {:reply-fn broadcast-to-others
                                                  :reply    (str username ": " income-msg)}))))

(defn fallback
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (if (str/starts-with? income-msg "/")
    (xiana/ok
      (update state :response-data merge {:reply-fn send-multi-line
                                          :reply    (str "Invalid command: " income-msg)}))
    (broadcast state)))
