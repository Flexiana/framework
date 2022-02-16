(ns state
  (:require
    [clojure.tools.namespace.repl :refer [disable-reload!]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]))

(disable-reload!)

(defonce dev-sys (atom (closeable-map {})))
