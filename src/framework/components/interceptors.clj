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

(defn message
  [m]
  {:enter (fn [s]
            (println m s)
            (xiana/ok s))
   :leave (fn [s]
            (println m s)
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
                  session-id (try (UUID/fromString (get-in request [:headers :session-id]))
                                  (catch Exception _ nil))
                  session (when session-id (fetch sessions-backend session-id))]
              (if session
                (xiana/ok (assoc state :session-data session))
                (xiana/ok (-> (assoc-in state [:session-data :session-id] (UUID/randomUUID))
                              (assoc-in [:session-data :new-session] true))))))

   :leave (fn [state]
            (let [sessions-backend (-> state
                                       :deps
                                       :session-backend)
                  session-id (get-in state [:session-data :session-id])]
              (add! sessions-backend session-id (dissoc (:session-data state) :new-session))
              (xiana/ok (assoc-in state [:response :headers "Session-id"] (str session-id)))))})

(def params
  "Extract parameters from request, should be middleware, or interceptor"
  {:enter (fn [state]
            (xiana/ok
              (clojure.core/update state :request
                #(keywordize-keys ((par/wrap-params identity) %)))))})

(defn db-access
  ([]
   {:leave (fn [{query :query
                 :as   state}]
             (if query
               (xiana/ok (let [result (db/execute state (sql/format query))]
                           (assoc-in state [:response-data :db-data] result)))
               (xiana/ok state)))})
  ([on-new-session]
   (assoc (db-access)
     :enter (fn [{{new-session :new-session} :session-data
                  :as                        state}]
              (if new-session
                (on-new-session state)
                (xiana/ok state))))))

(def view
  {:leave (fn [{view :view :as state}]
            (view state))})

(defn muuntaja
  ([]
   (muuntaja m-int/muun-instance))
  ([instance]
   (wrap/interceptor instance)))

(defn acl-restrict
  ([]
   (acl-restrict nil))
  ([m]
   {:enter (fn [state]
             (acl/is-allowed state m))
    :leave (fn [{query                 :query
                 {{user-id :id} :user} :session-data
                 owner-fn              :owner-fn
                 :as                   state}]
             (xiana/ok (if owner-fn
                         (assoc state :query (owner-fn query user-id))
                         state)))}))

(def side-effect
  {:leave (fn [{f :side-effect :as state}]
            (if f
              (f state)
              (xiana/ok state)))})
