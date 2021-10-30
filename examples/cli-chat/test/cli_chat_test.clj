(ns cli-chat-test
  (:require
    [cli-chat-fixture :refer [std-system-fixture]]
    [http.async.client :as a-client]
    [cli-chat.core]
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [clojure.string :as str])
  (:import (java.util UUID)))

(def config {})

(use-fixtures :once (partial std-system-fixture config))

(deftest index-test
  (is (= {:body   "Index page"
          :status 200}
         (-> {:url                  "http://localhost:3333/"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body])))))

(defn wait-for
  ([atom predict]
   (wait-for atom predict 5000))
  ([atom predict timeout]
   (loop [t 0]
     (cond (predict atom) atom
           (< t timeout) (do (Thread/sleep 100)
                             (recur (+ 100 t)))
           :else atom))))

(deftest http-async
  (with-open [client-1 (a-client/create-client)
              client-2 (a-client/create-client)]
    (let [response-1 (atom "")
          response-2 (atom "")
          session-id (str (UUID/randomUUID))
          ws-1 (a-client/websocket client-1 "ws://localhost:3333/chat"
                                   :headers {:session-id session-id}
                                   :text (fn [con mesg]
                                           (reset! response-1 mesg))
                                   :close (fn [con & status]
                                            (println "close:" con status))
                                   :error (fn [& args]
                                            (println "ERROR:" args))
                                   :open (fn [con]
                                           (println "opened:" con)))
          ws-2 (a-client/websocket client-2 "ws://localhost:3333/chat"
                                   :headers {:session-id session-id}
                                   :text (fn [con mesg]
                                           (reset! response-2 mesg))
                                   :close (fn [con & status]
                                            (println "close:" con status))
                                   :error (fn [& args]
                                            (println "ERROR:" args))
                                   :open (fn [con]
                                           (println "opened:" con)))]
      (a-client/send ws-1 :text session-id)
      (is (-> @(wait-for response-2 #(str/ends-with? @% session-id))
              (str/ends-with? session-id))))))
