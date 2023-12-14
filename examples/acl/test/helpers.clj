(ns helpers
  (:require
    [clj-http.client :refer [request]]
    [clojure.data.json :refer [write-str]]
    [jsonista.core :as j]))

(def test_member "611d7f8a-456d-4f3c-802d-4d869dcd89bf")
(def test_admin "b651939c-96e6-4fbb-88fb-299e728e21c8")
(def test_suspended_admin "b01fae53-d742-4990-ac01-edadeb4f2e8f")
(def test_staff "75c0d9b2-2c23-41a7-93a1-d1b716cdfa6c")

(defn delete
  ([uri]
   (request {:unexceptional-status (constantly true)
             :method               :delete
             :headers              {"Authorization" test_admin}
             :url                  (format "http://localhost:3333/%s" (name uri))}))
  ([uri user id]
   (request {:unexceptional-status (constantly true)
             :method               :delete
             :headers              {"Authorization" user}
             :url                  (format "http://localhost:3333/%s" (name uri))
             :query-params         {:id id}})))

(defn fetch
  ([uri user id]
   (request {:unexceptional-status (constantly true)
             :method               :get
             :headers              {"Authorization" user}
             :url                  (format "http://localhost:3333/%s" (name uri))
             :query-params         {:id id}}))
  ([uri user]
   (request {:unexceptional-status (constantly true)
             :method               :get
             :headers              {"Authorization" user}
             :url                  (format "http://localhost:3333/%s" (name uri))}))
  ([uri]
   (request {:unexceptional-status (constantly true)
             :method               :get
             :headers              {"Authorization" test_admin}
             :url                  (format "http://localhost:3333/%s" (name uri))})))

(defn put
  ([uri content]
   (request {:unexceptional-status (constantly true)
             :method               :put
             :headers              {"Authorization" test_admin
                                    "Content-Type"  "application/json;charset=utf-8"}
             :url                  (format "http://localhost:3333/%s" (name uri))
             :body                 (j/write-value-as-string content)}))
  ([uri user content]
   (request {:unexceptional-status (constantly true)
             :method               :put
             :headers              {"Authorization" user
                                    "Content-Type"  "application/json;charset=utf-8"}
             :url                  (format "http://localhost:3333/%s" (name uri))
             :body                 (j/write-value-as-string content)})))

(defn post
  [uri user id content]
  (request {:unexceptional-status (constantly true)
            :method               :post
            :headers              {"Authorization" user
                                   "Content-Type"  "application/json;charset=utf-8"}
            :url                  (format "http://localhost:3333/%s" (name uri))
            :query-params         {:id id}
            :body                 (j/write-value-as-string content)}))

