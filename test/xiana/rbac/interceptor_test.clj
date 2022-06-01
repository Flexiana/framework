(ns xiana.rbac.interceptor-test
  (:require
    [clojure.test :refer :all]
    [honeysql.helpers :as sql]
    [tiny-rbac.builder :as b]
    [xiana.rbac :refer [interceptor]]
    [xiana.session :as session])
  (:import
    (java.util
      UUID)))

(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)
      (b/add-inheritance :mixed :member)
      (b/add-permission :mixed :image :delete :all)))

(def guest
  {:users/role :guest
   :users/id   (str (UUID/randomUUID))})

(def member
  {:users/role :member
   :users/id   (str (UUID/randomUUID))})

(def mixed
  {:users/role :mixed
   :users/id   (str (UUID/randomUUID))})

(defn state
  [user permission]
  (let [session-id (str (UUID/randomUUID))
        session-backend (:session-backend (session/init-backend {}))]
    (session/add! session-backend session-id user)
    (-> (assoc-in {} [:request-data :permission] permission)
        (assoc-in [:request :headers "session-id"] session-id)
        (assoc-in [:deps :session-backend] session-backend)
        (assoc :session-data user)
        (assoc-in [:session-data :session-id] session-id)
        (assoc-in [:deps :role-set] role-set))))

(deftest user-permissions
  (is (= #{:image/all}
         (-> ((:enter interceptor) (state guest :image/download))
             :request-data
             :user-permissions)))
  (is (= #{:image/all}
         (-> ((:enter interceptor) (state member :image/download))
             :request-data
             :user-permissions)))
  (is (= #{:image/all}
         (-> ((:enter interceptor) (state member :image/upload))
             :request-data
             :user-permissions)))
  (is (= #{:image/own}
         (-> ((:enter interceptor) (state member :image/delete))
             :request-data
             :user-permissions)))
  (is (thrown-with-msg? Exception #"Forbidden"
        (-> ((:enter interceptor) (state guest :image/upload))
            :response))))

(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/all) state
      (user-permissions :image/own)
      (let [session-id (get-in state [:request :headers "session-id"])
            session-backend (-> state :deps :session-backend)
            user-id (:users/id (session/fetch session-backend session-id))]
        (update state :query sql/merge-where [:= :owner.id user-id]))
      :else (throw (ex-info "Invalid permission request"
                            {:status 403 :body "Invalid permission request"})))))

(defn action [state image-id]
  (-> state
      (assoc :query {:delete [:*]
                     :from   [:images]
                     :where  [:= :id image-id]})
      (assoc-in [:request-data :restriction-fn] restriction-fn)))

(deftest restrictions
  (let [user member
        image-id (str (UUID/randomUUID))]
    (is (= {:delete [:*],
            :from   [:images],
            :where  [:and
                     [:= :id image-id]
                     [:= :owner.id (:users/id user)]]}
           (-> (state user :image/delete)
               ((:enter interceptor))
               (action image-id)
               ((:leave interceptor))
               :query))
        "Add filter if user has restricted to ':own'"))
  (let [user guest
        image-id (str (UUID/randomUUID))]
    (is (thrown-with-msg? Exception #"Forbidden"
          (-> (state user :image/delete)
              ((:enter interceptor))
              (action image-id)
              ((:leave interceptor))
              :response)
          "Returns 403: Forbidden if action is forbidden"))
    (let [user mixed
          image-id (str (UUID/randomUUID))]
      (is (= {:delete [:*],
              :from   [:images],
              :where  [:= :id image-id]}
             (-> (state user :image/delete)
                 ((:enter interceptor))
                 (action image-id)
                 ((:leave interceptor))
                 :query))
          "Processing multiple restrictions in order")
      (let [state (-> (state {} :image/delete)
                      (assoc-in [:request-data :user-permissions] #{:image/none})
                      (assoc-in [:request-data :restriction-fn] restriction-fn))]
        (is (thrown-with-msg? Exception #"Invalid permission request"
              (->
                ((:leave interceptor) state)
                :response))
            "Throws exception when user-permission missing")))))
