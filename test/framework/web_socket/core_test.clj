(ns framework.web-socket.core-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [framework-fixture :as fixture]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.session.core :as session]
    [http.async.client :as a-client]
    [org.httpkit.client :as client]
    [org.httpkit.server :as server]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn echo [{req :request :as state}]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive (fn [ch msg]
                             (println "Message: " msg)
                             (server/send! ch msg))
               :on-open    #(println "OPEN: - - - - - " %)
               :on-ping    (fn [ch data]
                             (println "Ping"))
               :on-close   (fn [ch status]
                             (println "\nCLOSE=============="))
               :init       (fn [ch]
                             (prn "INIT: " ch)
                             (prn "Session-Id: " (get-in req [:headers :session-id])))})))

(defn hello
  [state]
  (xiana/ok (assoc state :response {:status 200
                                    :body   "Hello from REST!"})))

(def routes
  [["/ws" {:ws-action echo
           :action    hello}]])

(def backend
  (session/init-in-memory))

(def system-config
  {:routes                   routes
   :session-backend          backend
   :framework.app/web-server (config/get-spec :framework.app/web-server)
   :web-socket-interceptors  [interceptors/params]
   :controller-interceptors  [interceptors/params
                              (session/interceptor "/api" "/login")
                              rbac/interceptor]})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(deftest http-async
  (with-open [client (a-client/create-client)]
    (let [latch (promise)
          session-id (str (UUID/randomUUID))
          ws (a-client/websocket client "ws://localhost:3333/ws"
                                 :headers {:session-id session-id}
                                 :text (fn [con mesg]
                                         (deliver latch mesg))
                                 :close (fn [con & status]
                                          (println "close:" con status))
                                 :error (fn [& args]
                                          (println "ERROR:" args))
                                 :open (fn [con]
                                         (println "opened:" con)))]
      (a-client/send ws :text session-id)
      (is (= session-id @latch))
      (a-client/close ws))))

(deftest rest-call
  (is (= {:status 200, :body "Hello from REST!"}
         (-> @(client/get "http://localhost:3333/ws")
             (select-keys [:status :body])
             (update :body slurp)))))
