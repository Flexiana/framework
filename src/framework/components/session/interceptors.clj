(ns framework.components.session.interceptors
  (:require
    [framework.components.session.backend :refer [init-in-memory-session]]))

(defn init-in-memory-interceptor
  []
  (fn [ctx]
    {:enter (assoc ctx
              :session-backend
              (init-in-memory-session))}))
