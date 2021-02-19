(ns status-test
  (:require
    [app]
    [clj-http.client :as client]
    [clojure.test :refer :all]
    [com.stuartsierra.component :as component]
    [controllers.home-ctrl :as home-ctrl]
    [controllers.records-ctrl :as records-ctrl]
    [framework.config.core :as config]
    [framework.db.storage :as db.storage]
    [kerodon.core :refer :all]
    [kerodon.test :refer :all]
    [middlewares.core :as m]
    [router]
    [tongue.core :as tongue]
    [view.core :as xview]
    [view.templates.dictionaries :as dicts]
    [view.templates.home :as home-temp]
    [view.templates.layout :as lay]
    [view.templates.records :as record-temp]
    [web-server]
    [xiana.core :as xiana]))

(declare ^:dynamic *system*)

(def home-dict-fn
  (tongue/build-translate dicts/home-dict))

(def record-dict-fn
  (tongue/build-translate dicts/records-dict))

(defn system-fixture
  [f]
  (let [config (config/edn)
        app-cfg (:framework.app/ring config)
        web-server-cfg (:framework.app/web-server config)
        system (->
                 (component/system-map
                   :config config
                   :router (router/make-router)
                   :app (app/make-app app-cfg)
                   :web-server (web-server/make-web-server web-server-cfg))
                 (component/system-using
                   {:app    [:router]
                    :web-server [:app]})
                 (component/start-system))]
    (try
      (binding [*system* system]
        (println system)
        (f))
      (finally
        (binding [*system* nil]
          (component/stop-system system))))))

;; (use-fixtures :each system-fixture)

(defmacro gen-handler
  [pre-route pre-ctrl post-ctrl]
  `(fn [http-request#]
     (->
       (xiana.core/flow->
         (app/create-empty-state)
         (app/add-deps {:router router :db db})
         (app/add-http-request http-request#)
         '~pre-route
         (app/route)
         '~pre-ctrl
         (app/run-controller)
         '~post-ctrl)
       (xiana.core/extract)
       :response)))

(defn start-system
  []
  (let [config (config/edn)
        app-cfg (:framework.app/ring config)
        web-server-cfg (:framework.app/web-server config)]
    (->
      (component/system-map
        :config config
        :router (router/make-router)
        :app (app/make-app app-cfg)
        :web-server (web-server/make-web-server web-server-cfg))
      (component/system-using
        {:app    [:router]
         :web-server [:app]})
      (component/start-system))))

(deftest test-home-auto-langs-en
  (let [controller (fn [state]
                     (xiana/flow-> state
                                   (xview/is-html)
                                   (xview/auto-set-lang)
                                   (xview/set-layout lay/layout)
                                   (xview/set-template home-temp/home)
                                   (xview/set-dict dicts/home-dict)
                                   (xview/set-response home-ctrl/home-response)
                                   (xview/render)))
        temp-route [["/" {:controller controller}]]
        system (-> (with-redefs [router/routes temp-route]
                     (start-system))
                   (assoc-in [:request-data :controller] controller))
        date-str (str (home-dict-fn :en :date (java.util.Date.)))]
    (try
      (-> (session (get-in system [:app :handler]))
          (visit "/"
                 :request-method :get
                 :headers {"Content-type" "text/html"
                           "Accept-Language" "en"})
          (has (status? 200))
          (within [:h1]
                  (has (text? "Hello from Xiana framework!")))
          (within [:div#date]
                  (has (text? date-str))))
      (finally
        (component/stop-system system)))))

(deftest test-home-auto-langs-fr
  (let [controller (fn [state]
                     (xiana/flow-> state
                                   (xview/is-html)
                                   (xview/auto-set-lang)
                                   (xview/set-layout lay/layout)
                                   (xview/set-template home-temp/home)
                                   (xview/set-dict dicts/home-dict)
                                   (xview/set-response home-ctrl/home-response)
                                   (xview/render)))
        temp-route [["/" {:controller controller}]]
        system (with-redefs [router/routes temp-route]
                 (start-system))]
    (try
      (-> (session (get-in system [:app :handler]))
          (visit "/"
                 :request-method :get
                 :headers {"Content-type" "text/html"
                           "Accept-Language" "fr"})
          (has (status? 200))
          (within [:h1]
                  (has (text? "Bonjour de Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :fr :date (java.util.Date.))))))
      (finally
        (component/stop-system system)))))

(deftest test-home-user-langs-en
  (let [controller (fn [state]
                     (xiana/flow-> state
                                   (xview/is-html)
                                   (xview/set-lang :en)
                                   (xview/set-layout lay/layout)
                                   (xview/set-template home-temp/home)
                                   (xview/set-dict dicts/home-dict)
                                   (xview/set-response home-ctrl/home-response)
                                   (xview/render)))
        temp-route [["/" {:controller controller}]]
        system (with-redefs [router/routes temp-route]
                 (start-system))]
    (try
      (-> (session (get-in system [:app :handler]))
          (visit "/")
          (has (status? 200))
          (within [:h1]
                  (has (text? "Hello from Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :en :date (java.util.Date.))))))
      (finally
        (component/stop-system system)))))

(deftest test-home-user-langs-fr
  (let [controller (fn [state]
                     (xiana/flow-> state
                                   (xview/is-html)
                                   (xview/set-lang :fr)
                                   (xview/set-layout lay/layout)
                                   (xview/set-template home-temp/home)
                                   (xview/set-dict dicts/home-dict)
                                   (xview/set-response home-ctrl/home-response)
                                   (xview/render)))
        temp-route [["/" {:controller controller}]]
        system (with-redefs [router/routes temp-route]
                 (start-system))]
    (try
      (-> (session (get-in system [:app :handler]))
          (visit "/")
          (has (status? 200))
          (within [:h1]
                  (has (text? "Bonjour de Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :fr :date (java.util.Date.))))))
      (finally
        (component/stop-system system)))))

(deftest test-home-qparms-langs-en
  (let [controller (fn [state]
                     (xiana/flow-> state
                                   (xview/is-html)
                                   (xview/set-lang-by-query-params)
                                   (xview/set-layout lay/layout)
                                   (xview/set-template home-temp/home)
                                   (xview/set-dict dicts/home-dict)
                                   (xview/set-response home-ctrl/home-response)
                                   (xview/render)))
        temp-route [["/{language}" {:controller controller}]]
        system (with-redefs [router/routes temp-route]
                 (start-system))]
    (try
      (-> (session (get-in system [:app :handler]))
          (visit "/en")
          (has (status? 200))
          (within [:h1]
                  (has (text? "Hello from Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :en :date (java.util.Date.)))))
          (follow "fr")
          (within [:h1]
                  (has (text? "Bonjour de Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :fr :date (java.util.Date.))))))
      (finally
        (component/stop-system system)))))

(deftest test-home-qparams-langs-fr
  (let [controller (fn [state]
                     (xiana/flow-> state
                                   (xview/is-html)
                                   (xview/set-lang-by-query-params)
                                   (xview/set-layout lay/layout)
                                   (xview/set-template home-temp/home)
                                   (xview/set-dict dicts/home-dict)
                                   (xview/set-response home-ctrl/home-response)
                                   (xview/render)))
        temp-route [["/{language}" {:controller controller}]]
        system (with-redefs [router/routes temp-route]
                 (start-system))]
    (try
      (-> (session (get-in system [:app :handler]))
          (visit "/fr")
          (has (status? 200))
          (within [:h1]
                  (has (text? "Bonjour de Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :fr :date (java.util.Date.)))))
          (follow "en")
          (within [:h1]
                  (has (text? "Hello from Xiana framework!")))
          (within [:div#date]
                  (has (text? (home-dict-fn :en :date (java.util.Date.))))))
      (finally
        (component/stop-system system)))))

;; (deftest post-ctrl-mid-test
;;   (let [controller (fn [state]
;;                      (xiana/flow-> state
;;                                    (xview/is-html)
;;                                    (xview/auto-set-lang)
;;                                    (xview/set-layout lay/layout)
;;                                    (xview/set-template home-temp/home)
;;                                    (xview/set-dict dicts/home-dict)
;;                                    (xview/set-response home-ctrl/home-response)
;;                                    (xview/render)))
;;         temp-route [["/" {:controller controller}]]
;;         dict-fn (str (home-dict-fn :fr :date (java.util.Date.)))
;;         new-post-mid (fn [state] (xiana.core/flow-> state
;;                                                    (m/change-lang-middleware)
;;                                               ;; (xview/generate-response)
;;                                               (xiana/ok)))
;;         system (with-redefs [router/routes temp-route
;;                              middlewares.core/pre-controller-middlewares new-post-mid]
;;                  (start-system))]
;;     (try
;;       (-> (session (get-in system [:app :handler]))
;;           (visit "/"
;;                  :request-method :get
;;                  :headers {"Content-Type" "text/html"
;;                            "Accept-Language" "en"})
;;           (has (status? 200))
;;           (within [:h1]
;;                   (has (text? "Bonjour de Xiana framework!")))
;;           ;; (within [:div#date]
;;           ;;         (has (text? dict-fn)))
;;           )
;;       (finally
;;           (component/stop-system system)))))
