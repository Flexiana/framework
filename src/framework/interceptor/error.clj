(ns framework.interceptor.error)

(def handle-ex-info
  {:error (fn [state]
            (if-let [resp (-> state :exception ex-data)]
              (-> state
                  (assoc :response resp)
                  (dissoc :exception))
              state))})
