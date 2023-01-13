(ns jwt-fixture
  (:require
    [app.core :refer [app-cfg ->system]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]))

(defn std-system-fixture
  [f]
  (with-open [_ (-> app-cfg
                    ->system
                    closeable-map)]
    (f)))
