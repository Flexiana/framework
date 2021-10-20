(ns framework.web-socket.core-test
  (:require
    [clojure.test :refer :all]
    [framework-fixture :as fixture]
    [clojure.pprint :refer [pprint]]
    [framework.config.core :as config]
    [framework.handler.core :refer [handler-fn]]
    [framework.interceptor.core :as interceptors]
    [framework.rbac.core :as rbac]
    [framework.session.core :as session]
    [honeysql.helpers :as sql]
    [xiana.core :as xiana]
    [org.httpkit.server :as s]
    [http.async.client :as ac]
    [org.httpkit.client :as client])
  (:import (org.eclipse.jetty.websocket.client WebSocketClient)))

(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/all) (xiana/ok state)
      (user-permissions :image/own) (xiana/ok
                                      (let [session-id (get-in state [:request :headers "session-id"])
                                            session-backend (-> state :deps :session-backend)
                                            user-id (:users/id (session/fetch session-backend session-id))]
                                        (update state :query sql/merge-where [:= :owner.id user-id]))))))

(defn delete-action [state]
  (xiana/ok
    (-> state
        (assoc :query {:delete [:*]
                       :from   [:images]
                       :where  [:= :id (get-in state [:params :image-id])]})
        (assoc-in [:request-data :restriction-fn] restriction-fn))))

;(defn async-action)



(defn my-chatroom-handler
  [request]
  (if-not (:websocket? request)
    {:status 200 :body "Not in WebSocket"}
    (s/as-channel request
                  {:on-receive (fn [ch message] (println "on-receive:" message))
                   :on-close   (fn [ch status] (println "on-close:" ch status))
                   :on-open    (fn [ch] (println "on-open:" ch))})))

(defn echo [req]
  (println req)
  (s/as-channel req
                {:on-receive (fn [ch mesg]
                               (s/send! ch mesg))
                 :on-open    #(println "OPEN: - - - - - " %)
                 :on-close   (fn [ch status]
                               (println "\nCLOSE==============")
                               (pprint ch))
                 :init       (fn [ch] (println "INIT: " ch))}))

(def routes
  [["/api" {:handler handler-fn}
    ["/socket" {:action     delete-action
                :permission :image/delete}]]
   ["/ws" {:action echo}]])

(def backend
  (session/init-in-memory))


(def is-websocket?
  {})

(def system-config
  {:routes                   routes
   :session-backend          backend
   :framework.app/web-server (config/get-spec :framework.app/web-server)
   :controller-interceptors  [is-websocket?
                              interceptors/params
                              (session/interceptor "/api" "/login")
                              rbac/interceptor]})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(deftest http-async
  (with-open [client (ac/create-client)]
    (let [latch (promise)
          received-msg (atom nil)
          ws (ac/websocket client "ws://localhost:3333/ws"
                           :text (fn [con mesg]
                                   (reset! received-msg mesg)
                                   (deliver latch true))
                           :close (fn [con & status]
                                    (println "close:" con status))
                           :error (fn [con & args]
                                    (println "ERROR:" con args))
                           :open (fn [con]
                                   (println "opened:" con)))]

      ;; (h/send ws :byte (byte-array 10)) not implemented yet
      (let [msg "testing12"]
        (ac/send ws :text msg)
        @latch
        (println)
        (is (= msg @received-msg)))
      (ac/close ws))))
