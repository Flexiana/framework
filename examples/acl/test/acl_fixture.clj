(ns acl-fixture
  (:require
    [acl]))

(defn std-system-fixture
  [f]
  (with-open [_ (acl/->system acl/app-cfg)]
    (f)))

