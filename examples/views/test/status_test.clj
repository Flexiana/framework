(ns status-test
  (:require
   [backend.components.app]
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [view.templates.dictionaries :as dicts]
   [view.templates.home :as home-temp]
   [view.templates.layout :as lay]
   [view.templates.records :as record-temp]
    [framework.config.core :as config]
    [kerodon.core :refer :all]
    [kerodon.test :refer :all]))



;; (deftest home-page
;;   (testing "en"
;;     (let [title (get-in (dicts/home-dict) [:en :greet-title])
;;           app-cfg (:framework.app/ring (config/edn))
;;           handler (app/ring-app app-cfg)]
;;       (-> (session handler)
;;           (visit "/")
;;           (has (status? 200))
;;           (has (some-text? title))))))
