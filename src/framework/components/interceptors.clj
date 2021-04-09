(ns framework.components.interceptors
  (:require
    [clojure.core :as core]
    [clojure.walk :refer [keywordize-keys]]
    [framework.acl.core :as acl]
    [framework.components.interceptors.muuntaja :as m-int]
    [framework.components.interceptors.wrap :as wrap]
    [framework.components.session.backend :refer [fetch add!]]
    [framework.db.sql :as db]
    [honeysql.core :as sql]
    [honeysql.helpers :as helpers]
    [ring.middleware.params :as par]
    [xiana.core :as xiana])
  (:import
    (java.util
      UUID)))

(def log
  {:enter (fn [s]
            (println "Enter to controller" s)
            (xiana/ok s))
   :leave (fn [s]
            (println "Leave from controller" s)
            (xiana/ok s))})

(defn- guest
  [state]
  (xiana/ok (assoc-in state [:session-data :user] {:id   (UUID/randomUUID)
                                                   :role :guest})))

(defn require-logged-in
  ([]
   (require-logged-in guest))
  ([or-else]
   {:enter (fn [{req :request :as state}]
             (if-let [authorization (get-in req [:headers :authorization])]
               (try (xiana/ok (-> (assoc-in state [:session-data :authorization] authorization)
                                  (assoc-in [:session-data :user :id] (UUID/fromString authorization))))
                    (catch IllegalArgumentException _ (or-else state)))
               (or-else state)))}))

(def session-interceptor
  {:enter (fn [{request :request :as state}]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id (get-in request [:headers :session-id])
                  user (when session-id (fetch sessions-backend session-id))]
              (if user
                (xiana/ok (assoc-in state [:session-data :user] user))
                (xiana/ok (-> (assoc-in state [:session-data :session-id] (UUID/randomUUID))
                              (assoc-in [:session-data :new-session] true))))))

   :leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id (get-in state [:session-data :session-id])]
              (add! sessions-backend session-id (get-in state [:session-data :user]))
              (xiana/ok (assoc-in state [:response :headers "Session-id"] (str session-id)))))})

(def params
  "Extract parameters from request, should be middleware, or interceptor"
  {:enter (fn [state]
            (xiana/ok
              (clojure.core/update state :request
                #(keywordize-keys ((par/wrap-params identity) %)))))})

(defn- purify
  "Removes namespaces from keywords"
  [elem]
  (into {}
        (map (fn [[k v]] {(keyword (name k)) v}) elem)))

(defn- load-user
  [{{{user-id :id} :user} :session-data
    :as                   state}]
  (let [query (-> (helpers/select :*)
                  (helpers/from :users)
                  (helpers/where [:= :id user-id])
                  sql/format)
        user (first (db/execute state query))]
    (if user
      (xiana/ok (-> (assoc-in state [:session-data :user] (purify user))
                    (core/update :session-data dissoc :new-session)))
      state)))

(defn db-access
  ([]
   (db-access load-user))
  ([on-new-session]
   {:enter (fn [{{new-session :new-session} :session-data
                 :as                        state}]
             (if new-session
               (on-new-session state)
               (xiana/ok state)))
    :leave (fn [{query :query
                 :as   state}]
             (xiana/ok (let [result (db/execute state (sql/format query))]
                         (assoc-in state [:response-data :db-data] result))))}))

(def view
  {:leave (fn [state]
            ((:view state) state))})

(defn muuntaja
  ([]
   (muuntaja m-int/muun-instance))
  ([instance]
   (wrap/interceptor instance)))

(defn acl-restrict
  ([]
   (acl-restrict nil))
  ([or-else]
   {:enter (fn [state]
             (acl/is-allowed state {:or-else or-else}))
    :leave (fn [{query                 :query
                 {{user-id :id} :user} :session-data
                 owner-fn              :owner-fn
                 :as                   state}]
             (xiana/ok (if owner-fn
                         (assoc state :query (owner-fn query user-id))
                         state)))}))
