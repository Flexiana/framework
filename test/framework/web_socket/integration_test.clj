(ns framework.web-socket.integration-test
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
    [taoensso.timbre :as log]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(defn echo [{req :request :as state}]
  (xiana/ok
    (assoc-in state [:response-data :channel]
              {:on-receive (fn [ch msg]
                             (log/info "Message: " msg)
                             (server/send! ch msg))
               :on-open    #(log/info "OPEN: - - - - - " %)
               :on-ping    (fn [ch data]
                             (log/info "Ping"))
               :on-close   (fn [ch status]
                             (log/info "\nCLOSE=============="))
               :init       (fn [ch]
                             (log/info "INIT: " ch)
                             (log/info "Session-Id: " (get-in req [:headers :session-id])))})))

(defn hello
  [state]
  (xiana/ok (assoc state :response {:status 200
                                    :body   "Hello from REST!"})))

(def routes
  [["/ws" {:ws-action echo
           :action    hello}]])

(def system-config
  {:routes                  routes
   :web-socket-interceptors [interceptors/params]
   :controller-interceptors [interceptors/params
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
                                          (log/info "close:" con status))
                                 :error (fn [& args]
                                          (log/info "ERROR:" args))
                                 :open (fn [con]
                                         (log/info "opened:" con)))]
      (a-client/send ws :text session-id)
      (is (= session-id @latch))
      (a-client/close ws))))

(deftest rest-call
  (is (= {:status 200, :body "Hello from REST!"}
         (-> @(client/get "http://localhost:3333/ws")
             (select-keys [:status :body])
             (update :body slurp)))))
