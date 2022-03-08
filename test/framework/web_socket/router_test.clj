(ns framework.web-socket.router-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [framework.interceptor.core :as interceptors]
    [framework.websockets.core :refer [router string->]]
    [reitit.core :as r]))

(defn string-log [state]
  (assoc state :response-data {:reply-fn identity
                               :reply    "Log was called via string"}))

(defn json-log [state]
  (assoc state :response-data {:reply-fn identity
                               :reply    "Log was called via json"}))

(defn log-edn [state]
  (assoc state :response-data {:reply-fn identity
                               :reply    "Log was called via edn"}))

(def routes
  (r/router [["/log-string" {:action string-log}]
             ["log-json" {:action json-log}]
             ["log-edn" {:action log-edn}]]
            {:data {:default-interceptors [(interceptors/message "Incoming message...")]}}))

(def routing
  (partial router routes))

(deftest router-test
  (let [string-action "/log-string"
        json-action (json/write-str {:action :log-json})
        edn-action "{:action :log-edn}"]
    (is (= "Log was called via string"
           (-> (routing {:request-data
                         {:income-msg string-action}})
               :response-data
               :reply)))
    (is (= "Log was called via json"
           (-> (routing {:request-data
                         {:income-msg json-action}})
               :response-data
               :reply)))
    (is (= "Log was called via edn"
           (-> (routing {:request-data
                         {:income-msg edn-action}})
               :response-data
               :reply)))))
