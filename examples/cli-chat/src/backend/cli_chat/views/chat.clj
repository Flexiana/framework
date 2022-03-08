(ns cli-chat.views.chat
  (:require
    [clojure.string :as str]
    [ring.adapter.jetty9 :as jetty]))

(defn send-multi-line
  ([ch reply]
   (doseq [m (str/split-lines reply)]
     (jetty/send! ch (str m "\n"))))
  ([{{req-ch :ch}    :request-data
     {res-ch :ch
      reply  :reply} :response-data}]
   (send-multi-line (or res-ch req-ch) reply)))

(defn broadcast-to-others
  [{{ch       :ch
     channels :channels} :request-data
    {reply :reply}       :response-data}]
  (doseq [c (remove #(#{ch} (key %)) @channels)]
    (jetty/send! (key c) reply)))

(defn broadcast
  [{{ch         :ch
     channels   :channels
     income-msg :income-msg} :request-data
    :as                      state}]
  (let [username (get-in @channels [ch :users/name])]
    (update state :response-data merge {:reply-fn broadcast-to-others
                                        :reply    (str username ": " income-msg)})))

(defn fallback
  [{{income-msg :income-msg} :request-data
    :as                      state}]
  (if (str/starts-with? income-msg "/")
    (update state :response-data merge {:reply-fn send-multi-line
                                        :reply    (str "Invalid command: " income-msg)})
    (broadcast state)))
