(ns framework.config.core
  (:require [integrant.core :as ig]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(defmethod aero/reader 'ig/ref
  [_ tag value]
  (ig/ref value))

(defn edn
  [profile]
  (:framework
   (aero/read-config (io/resource "main.edn") {:profile profile})))
