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
    [http.async.client :as ac]
    [org.httpkit.server :as s]))

(defn echo [req]
  (s/as-channel req
                {:on-receive (fn [ch msg]
                               (println "Message: " msg)
                               (s/send! ch msg))
                 :on-open    #(println "OPEN: - - - - - " %)
                 :on-ping    (fn [ch data]
                               (println "Ping"))
                 :on-close   (fn [ch status]
                               (println "\nCLOSE=============="))
                 :init       (fn [ch] (println "INIT: " ch))}))

(def routes
  [["/ws" {:action echo}]])

(def backend
  (session/init-in-memory))

(def system-config
  {:routes                   routes
   :session-backend          backend
   :framework.app/web-server (config/get-spec :framework.app/web-server)
   :controller-interceptors  [interceptors/params
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
                           :error (fn [& args]
                                    (println "ERROR:" args))
                           :open (fn [con]
                                   (println "opened:" con)))]

      ;; (h/send ws :byte (byte-array 10)) not implemented yet
      (let [msg "testing12"]
        (ac/send ws :text msg)
        @latch
        (println)
        (is (= msg @received-msg)))
      (ac/close ws))))
