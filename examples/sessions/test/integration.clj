(ns integration
  (:require
    [app.core :as app]
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [framework.config.core :as config]
    [framework.db.core :as db]
    [jsonista.core :as json]
    [next.jdbc :as jdbc]
    [xiana.commons :as commons])
  (:import
    (java.util
      UUID)))

(defn merge-connection
  [config]
  (assoc config :xiana/session-backend
         (commons/deep-merge (:xiana/session-backend config)
                             (:xiana/postgresql config))))

(defn std-system-fixture
  [f]
  (with-open [system (-> (config/config app/app-cfg)
                         (assoc :xiana/postgresql (:xiana/session-backend (config/config)))
                         (assoc-in [:xiana/postgresql :image-name] "postgres:14-alpine")
                         db/docker-postgres!
                         db/connect
                         merge-connection
                         app/->system)]
    (jdbc/execute! (jdbc/get-datasource (get-in system [:db :datasource])) [(slurp "init.sql")])
    (f)))

(use-fixtures :once std-system-fixture)

(deftest testing-without-login
  (is (= {:status 200, :body "Index page"}
         (-> {:url                  "http://localhost:3333/"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body])))
      "Index page is always available")
  (is (= {:status 401, :body "{\"message\":\"Invalid or missing session\"}"}
         (-> {:url                  "http://localhost:3333/secret"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body])))
      "Should login to see the secret page")
  (is (= {:status 401, :body "{\"message\":\"Invalid or missing session\"}"}
         (-> {:url                  "http://localhost:3333/wrong"
              :unexceptional-status (constantly true)
              :method               :get}
             http/request
             (select-keys [:status :body])))
      "A missing page is hidden too"))

(deftest login-fails-on-empty-body
  (let [login-request {:url                  "http://localhost:3333/login"
                       :unexceptional-status (constantly true)
                       :method               :post}
        login-response (http/request login-request)]
    (is (= {:status 401
            :body "Missing credentials"}
           (select-keys login-response [:status :body])))))

(deftest testing-with-login
  (let [login-request {:url                  "http://localhost:3333/login"
                       :unexceptional-status (constantly true)
                       :body                 (json/write-value-as-string
                                               {:email    "piotr@example.com"
                                                :password "topsecret"})
                       :method               :post}
        login-response (http/request login-request)
        login (-> login-response
                  :body
                  json/read-value)
        body-session-id (get login "session-id")
        header-session-id (get-in login-response [:headers "Session-id"])
        user (get login "user")]
    (is (= {"first-name" "Piotr", "id" 1, "email" "piotr@example.com", "last-name" "Developer"}
           user)
        "User has been logged in")
    (is (true? (try (UUID/fromString body-session-id)
                    true
                    (catch Exception _ false)))
        "and has UUID session id")
    (is (= body-session-id header-session-id)
        "Session ID is the same in the response and from headers")
    (is (= {:status 200, :body "Index page, for Piotr"}
           (-> {:url                  "http://localhost:3333/"
                :headers              {:session-id body-session-id}
                :unexceptional-status (constantly true)
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "Index page has some surprise when user logged in")
    (is (= {:status 200, :body "Hello Piotr"}
           (-> {:url                  "http://localhost:3333/secret"
                :unexceptional-status (constantly true)
                :headers              {:session-id body-session-id}
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "The secret page is visible with login")
    (is (= {:body   "Not Found"
            :status 404}
           (-> {:url                  "http://localhost:3333/wrong"
                :unexceptional-status (constantly true)
                :headers              {:session-id body-session-id}
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "A missing page is missing")))

(deftest testing-with-logout
  (let [login (-> {:url                  "http://localhost:3333/login"
                   :unexceptional-status (constantly true)
                   :body                 (json/write-value-as-string
                                           {:email    "piotr@example.com"
                                            :password "topsecret"})
                   :method               :post}
                  http/request
                  :body
                  json/read-value)
        session-id (get login "session-id")
        user (get login "user")]
    (is (= {"first-name" "Piotr", "id" 1, "email" "piotr@example.com", "last-name" "Developer"}
           user)
        "User is logged in")
    (is (true? (try (UUID/fromString session-id)
                    true
                    (catch Exception _ false)))
        "and has UUID session id")
    (is (= {:status 200 :body "Piotr logged out"}
           (-> {:url                  "http://localhost:3333/logout"
                :headers              {:session-id session-id}
                :unexceptional-status (constantly true)
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "User logged out")
    (is (= {:status 200, :body "Index page"}
           (-> {:url                  "http://localhost:3333/"
                :headers              {:session-id session-id}
                :unexceptional-status (constantly true)
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "Index page is always available")
    (is (= {:status 401, :body "{\"message\":\"Invalid or missing session\"}"}
           (-> {:url                  "http://localhost:3333/secret"
                :unexceptional-status (constantly true)
                :headers              {:session-id session-id}
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "The secret page gets hidden")
    (is (= {:status 401, :body "{\"message\":\"Invalid or missing session\"}"}
           (-> {:url                  "http://localhost:3333/wrong"
                :unexceptional-status (constantly true)
                :headers              {:session-id session-id}
                :method               :get}
               http/request
               (select-keys [:status :body])))
        "Do not reveal if a page is missing")))
