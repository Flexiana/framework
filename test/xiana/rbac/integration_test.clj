(ns xiana.rbac.integration-test
  (:require
    [clj-http.client :as http]
    [clojure.test :refer [deftest is use-fixtures]]
    [honeysql.helpers :as sql]
    [tiny-rbac.builder :as b]
    [xiana-fixture :as fixture]
    [xiana.handler :refer [handler-fn]]
    [xiana.interceptor :as interceptors]
    [xiana.rbac :as rbac]
    [xiana.session :as session])
  (:import
    (java.util
      UUID)))

(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/all) state
      (user-permissions :image/own) (let [session-id (get-in state [:request :headers "session-id"])]
                                        session-backend (-> state :deps :session-backend)
                                          user-id (:users/id (session/fetch session-backend session-id))
                                      (update state :query sql/merge-where [:= :owner.id user-id])))))

(defn delete-action [state]
  (-> state
      (assoc :query {:delete [:*]
                     :from   [:images]
                     :where  [:= :id (get-in state [:params :image-id])]})
      (assoc-in [:request-data :restriction-fn] restriction-fn)))

(def routes
  [["/api" {:handler handler-fn}
    ["/image" {:delete {:action     delete-action
                        :permission :image/delete}}]]])

(def backend
  (:session-backend (session/init-backend {})))

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
   :xiana/role-set          role-set
   :controller-interceptors [framework.interceptor.error/handle-ex-info
                             interceptors/params
                             session/interceptor
                             rbac/interceptor]})

(use-fixtures :once (partial fixture/std-system-fixture system-config))

(def guest
  {:users/role :guest
   :users/id   (str (UUID/randomUUID))})

(def member
  {:users/role :member
   :users/id   (str (UUID/randomUUID))})

(deftest delete-request-by-member
  (let [session-id (UUID/randomUUID)
        _          (session/add! backend session-id (assoc member :session-id session-id))
        response   (http/delete "http://localhost:3333/api/image"
                                {:throw-exceptions false}
                            :headers          {"Session-id" (str session-id)})]
    (is (= 200 (:status response)))))

(deftest delete-request-by-guest
  (let [session-id (UUID/randomUUID)
        _          (session/add! backend session-id (assoc guest :session-id session-id))
        response   (http/delete "http://localhost:3333/api/image"
                                {:throw-exceptions false}
                            :headers          {"Session-id" (str session-id)})]
    (is (= 403 (:status response)))))
