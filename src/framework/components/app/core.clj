(ns framework.components.app.core
  (:require
    [cats.core :as m]
    [com.stuartsierra.component :as component]
    [framework.acl.builder :as acl-builder]
    [framework.components.runner :as runner]
    [reitit.core :as r]
    [xiana.commons :refer [?assoc-in]]
    [xiana.core :as xiana]))

(defn create-empty-state
  []
  (xiana/map->State {}))

(defn add-deps
  [state deps]
  (xiana/ok
    (assoc state :deps deps)))

(defn add-session-backend
  [state session-backend]
  (xiana/ok (update state :deps conj session-backend)))

(defn add-http-request
  [state http-request]
  (xiana/ok
    (assoc state :request http-request)))

(defn response
  [state response]
  (assoc state :response response))

(defn default-action
  [{request :request {handler :handler} :request-data :as state}]
  (try
    (xiana/ok (assoc state :response (handler request)))
    (catch Exception e
      (xiana/error (-> state
                       (assoc :controller-error e)
                       (assoc :response {:status 500 :body "Internal Server error"}))))))

(defn route
  [{request          :request
    {router :router} :deps
    :as              state}]
  (let [match (r/match-by-path router (:uri request))
        method (:request-method request)
        handler (or (get-in match [:data :handler]) (-> match :result method :handler))
        action (or (get-in match [:data :action]) (-> match :data method :action))]
    (if action
      (xiana/ok (-> state
                    (?assoc-in [:request-data :match] match)
                    (?assoc-in [:request-data :handler] handler)
                    (assoc-in [:request-data :action] action)))

      (if handler
        (xiana/ok (-> state
                      (?assoc-in [:request-data :match] match)
                      (assoc-in [:request-data :handler] handler)
                      (assoc-in [:request-data :action] default-action)))
        (xiana/error (response state {:status 404 :body "Not Found"}))))))

(defn run-controller
  [state]
  (let [controller (get-in state [:request-data :action])]
    (controller state)))

(defn select-interceptors
  [interceptors context ordering]
  (->> interceptors
       (filter context)
       ordering
       (map context)
       (map #(fn [x] (% x)))))

(defn init-acl
  [this config]
  (if config
    (acl-builder/init this config)
    this))

(defrecord App
  [config acl-cfg session-backend router db]
  component/Lifecycle
  (stop [this] this)
  (start [this]
         (assoc this
           :handler
           (fn [http-request]
             (->
               (apply m/>>=
                 (concat
                   [(xiana.core/ok (create-empty-state))
                    (fn [x] (add-deps x {:router router, :db db}))
                    (fn [x] (add-session-backend x session-backend))
                    (fn [x] (add-http-request x http-request))
                    (fn [x] (init-acl x acl-cfg))]
                   (-> this :router-interceptors (select-interceptors :enter identity))
                   [(fn [x] (route x))]
                   (-> this :router-interceptors (select-interceptors :leave reverse))

                   (-> this :controller-interceptors (select-interceptors :enter identity))
                   [(fn [x] (run-controller x))]
                   (-> this :controller-interceptors (select-interceptors :leave reverse))))
               (xiana/extract)
               (get :response))))))

(defn make-app
  "DEPRECATED"
  [{config                  :config
    acl-cfg                 :acl-cfg
    session-backend         :session-backend
    router-interceptors     :router-interceptors
    controller-interceptors :controller-interceptors}]
  (map->App {:config                  config
             :acl-cfg                 acl-cfg
             :session-backend         session-backend
             :router-interceptors     router-interceptors
             :controller-interceptors controller-interceptors}))

(defn mbuild-state
  [{:keys [deps
           http-request
           acl-cfg]}]
  (m/>>= (xiana/ok (create-empty-state))
         (comp xiana/ok
               #(assoc %
                  :deps deps
                  :request http-request))
         #(if acl-cfg
            (acl-builder/init % acl-cfg)
            %)))

(defn state-build
  [{:keys [acl-cfg session-backend auth]}
   {:keys [router db]}
   http-request]
  (-> (xiana/map->State {:deps    {:router          (:router router)
                                   :db              db
                                   :session-backend session-backend
                                   :auth            auth}
                         :request http-request})
      (assoc :acl-cfg acl-cfg)))

(defn ->app
  [config]
  (with-meta config
    `{component/start ~(fn [{:keys [router-interceptors
                                    controller-interceptors]
                             :as   this}]
                         (assoc this
                           :handler
                           (fn [http-request]
                             (->
                               (xiana/flow->
                                 (state-build config this http-request)
                                 (runner/run router-interceptors route)
                                 (runner/run controller-interceptors run-controller))
                               (xiana/extract)
                               (get :response)))))
      component/stop  ~(fn [this]
                         (dissoc this :handler))}))
