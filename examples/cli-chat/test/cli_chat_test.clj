(ns cli-chat-test
  (:require
    [cli-chat-fixture :refer [std-system-fixture]]
    [cli-chat.core :refer [app-cfg]]
    [clj-http.client :as http]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.test :refer [deftest is use-fixtures]]
    [http.async.client :as a-client]))

(use-fixtures :once (partial std-system-fixture app-cfg))

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

(deftest websockets
  (with-open [client-1 (a-client/create-client)
              client-2 (a-client/create-client)]
    (let [response-1 (atom "")
          response-2 (atom "")
          ws-1 (a-client/websocket client-1 "ws://localhost:3333/chat"
                                   :text (fn [_ mesg]
                                           (reset! response-1 mesg)))
          ws-2 (a-client/websocket client-2 "ws://localhost:3333/chat"
                                   :text (fn [_ mesg]
                                           (reset! response-2 mesg)))
          user-1 (do
                   (a-client/send ws-1 :text "/me")
                   (edn/read-string (deref (wait-for response-1 (fn* [p1__1521233#] (str/ends-with? (deref p1__1521233#) "}\n"))))))
          user-2 (do
                   (a-client/send ws-2 :text "/me")
                   (edn/read-string (deref (wait-for response-2 (fn* [p1__1516199#] (str/ends-with? (deref p1__1516199#) "}\n"))))))
          user-name-1 (user-1 :users/name)
          user-name-2 (user-2 :users/name)]
      (is (.startsWith user-name-1 "guest_")
          "User-1's name is guest")
      (is (= :guest (user-1 :users/role))
          "User-1 is guest")
      (is (.startsWith user-name-2 "guest_")
          "User-2's name is a guest")
      (is (= :guest (user-2 :users/role))
          "User-2 is guest")
      (is (= (str user-name-1 ": Message to all")
             (do (a-client/send ws-1 :text "Message to all")
                 @(wait-for response-2 #(str/ends-with? @% "all"))))
          "User-2 gets message from user-1")
      (is (= (str ">>" user-name-1 ">>  Message to user-2\n")
             (do (a-client/send ws-1 :text (format "/to %s Message to user-2" user-name-2))
                 @(wait-for response-2 #(str/ends-with? @% "user-2\n"))))
          "User-2 gets direct message from user-1")
      (is (= "Invalid username or password\n"
             (do (a-client/send ws-1 :text "/login unknown unknown")
                 @(wait-for response-1 #(= @% "Invalid username or password\n"))))
          "Cannot login if user not exists")
      (is (= #:users{:id   1
                     :name "unknown"
                     :role "member"}
             (do (a-client/send ws-1 :text "/sign-up unknown unknown")
                 (edn/read-string (deref (wait-for response-1 (fn* [p1__1480433#] (str/ends-with? (deref p1__1480433#) "}\n")))))))
          "Can sign up")
      (is (= "Username already registered\n"
             (do (a-client/send ws-1 :text "/sign-up unknown password")
                 (-> @(wait-for response-1 #(= @% "Username already registered\n")))))
          "Can't sign up with the same username")
      (is (= "Invalid username or password\n"
             (do (a-client/send ws-2 :text "/login unknown password")
                 (-> @(wait-for response-2 #(= @% "Invalid username or password\n")))))
          "Can't login with wrong password")
      (is (= "Successful login\n"
             (do (a-client/send ws-2 :text "/login unknown unknown")
                 (-> @(wait-for response-2 #(= @% "Successful login\n")))))
          "Can login with right password"))))
