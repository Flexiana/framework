(ns acl-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.stuartsierra.component :as component]
    [components :as comps]
    [framework.config.core :as config]))

(def test_guest "")
(def test_customer "611d7f8a-456d-4f3c-802d-4d869dcd89bf")
(def test_admin "b651939c-96e6-4fbb-88fb-299e728e21c8")
(def test_suspended_admin "b01fae53-d742-4990-ac01-edadeb4f2e8f")
(def test_staff "75c0d9b2-2c23-41a7-93a1-d1b716cdfa6c")

(defn delete-all-posts
  []
  (-> {:url                  "http://localhost:3000/posts"
       :headers              {"Authorization" test_admin}
       :unexceptional-status (constantly true)
       :method               :delete}
      http/request))

(defn fetch-posts
  ([user id]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" user}
        :unexceptional-status (constantly true)
        :query-params         {:id id}
        :method               :get}
       http/request))
  ([user]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" user}
        :unexceptional-status (constantly true)
        :method               :get}
       http/request))
  ([]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" test_admin}
        :unexceptional-status (constantly true)
        :method               :get}
       http/request)))

(defn new-post
  ([content]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" test_admin}
        :unexceptional-status (constantly true)
        :form-params          {:content content}
        :method               :put}
       http/request))
  ([user content]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" user}
        :unexceptional-status (constantly true)
        :form-params          {:content content}
        :method               :put}
       http/request)))

(defn update-post
  ([id content]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" test_admin}
        :unexceptional-status (constantly true)
        :form-params          {:content content}
        :query-params         {:id id}
        :method               :put}
       http/request))
  ([user id content]
   (-> {:url                  "http://localhost:3000/posts"
        :headers              {"Authorization" user}
        :unexceptional-status (constantly true)
        :form-params          {:content content}
        :query-params         {:id id}
        :method               :put}
       http/request)))

(def post-id-regex
  #":posts\/id #uuid \"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12})")

(defn post-ids
  [body]
  (map last (re-seq post-id-regex body)))

(defn logit
  [x]
  (println x)
  x)

(defn all-post-ids
  []
  (-> (fetch-posts)
      :body
      post-ids))

(defn update-count
  [body]
  (-> (re-find #"update-count (\d+)" body)
      last
      Integer/parseInt))

(defn std-system-fixture
  [f]
  (let [config (config/edn)
        system (-> config
                   comps/system
                   component/start)]
    (try
      (delete-all-posts)
      (new-post "Test post")
      (new-post "Second Test post")
      (f)
      (finally
        (delete-all-posts)
        (component/stop system)))))

(use-fixtures :once std-system-fixture)

(deftest guest-can-read-posts
  (let [orig-ids (all-post-ids)]
    (is (= [(count orig-ids) orig-ids]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :method               :get}
               http/request
               :body
               post-ids
               ((juxt count identity)))) "Guest can read all posts")
    (is (= [1 (first orig-ids)]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :query-params         {:id (first orig-ids)}
                :method               :get}
               http/request
               :body
               post-ids
               ((juxt count first)))) "Guest can read post by id")))

(deftest guest-cannot-delete-posts
  (let [orig-ids (all-post-ids)]
    (is (= [401 "You don't have rights to do this"]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :method               :delete}
               http/request
               ((juxt :status :body)))) "Guest can read all posts")
    (is (= [401 "You don't have rights to do this"]
           (-> {:url                  "http://localhost:3000/posts"
                :unexceptional-status (constantly true)
                :query-params         {:id (first orig-ids)}
                :method               :delete}
               http/request
               ((juxt :status :body)))) "Guest can read post by id")))
