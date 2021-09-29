(ns framework.rbac.integration-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [framework-fixture :as fixture]
    [framework.rbac.core :as rbac]
    [framework.session.core :as session]
    [framework.session.core :as session-backend]
    [framework.webserver.core :as ws]
    [honeysql.helpers :as sql]
    [tiny-rbac.builder :as b]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(def owner-fns
  {:image/own (fn [state]
                (let [session-id (get-in state [:request :headers "Session-id"])
                      session-backend (-> state :deps :session-backend)
                      user-id (:users/id (session/fetch session-backend session-id))]
                  (update state :query sql/merge-where [:= :owner.id user-id])))
   :image/all identity})

(defn delete-action [state]
  (xiana/ok (-> state
                (assoc :query {:delete [:*]
                               :from   [:images]
                               :where  [:= :id (get-in state [:params :image-id])]})
                (assoc-in [:request-data :restriction-fns] owner-fns))))

(def routes
  [["/api" {:handler ws/handler-fn}
    ["/image" {:delete {:action     delete-action
                        :permission :image/delete}}]]])

(def backend
  (session-backend/init-in-memory))

(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)))

(def system-config
  {:routes                  routes
   :session-backend         backend
   :role-set                (:role-set (rbac/init role-set))
   :controller-interceptors [session/interceptor
                             rbac/interceptor]})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(def guest
  {:users/role :guest
   :users/id   (str (UUID/randomUUID))})

(def member
  {:users/role :member
   :users/id   (str (UUID/randomUUID))})

(deftest by-member
  (let [session-id (str (UUID/randomUUID))]
    (session/add! backend session-id member)
    (is (= 200 (:status (http/delete "http://localhost:3333/api/image"
                                     {:throw-exceptions false
                                      :headers          {"Session-id" session-id}}))))))

(deftest by-guest
  (let [session-id (str (UUID/randomUUID))]
    (session/add! backend session-id guest)
    (is (= 403 (:status (http/delete "http://localhost:3333/api/image"
                                     {:throw-exceptions false
                                      :headers          {"Session-id" session-id}}))))))
