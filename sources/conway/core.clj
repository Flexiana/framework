(ns conway.core
  (:require
    [conway.system :as system]
    [integrant.core :as ig]))


(defn -main
  []
  (-> system/model ig/prep ig/init))
