(ns jwt-fixture
  (:require
    [app.core :refer [app-cfg ->system]]
    [piotr-yuxuan.closeable-map :refer [closeable-map]]))

(defonce test-system (atom {}))

(defn std-system-fixture
  [f]
  (with-open [_ (reset! test-system
                        (-> app-cfg
                            ->system
                            closeable-map))]
    (f)
    (reset! test-system {})))
