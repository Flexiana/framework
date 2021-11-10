(ns cli-chat-fixture
  (:require
    [cli-chat.core :refer [->system]]))

(defn std-system-fixture
  [config f]
  (with-open [_ (->system config)]
    (f)))
