(ns framework.db.storage
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::postgresql
  [_ cfg]
  (println "Start Postgresql " cfg))

(defmethod ig/halt-key! ::postgresql
  [_ cfg]
  (println "Stop Postgresql"))
