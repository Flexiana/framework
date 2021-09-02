(ns framework.core
  (:require
    [framework.system :as system]
    [integrant.core :as ig]))


(defn -main
  []
  (-> system/model ig/prep ig/init))
