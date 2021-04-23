(ns framework.test-interceptors
  (:require
    [framework.acl.core :as acl]
    [xiana.core :as xiana]))

(def test-interceptor
  {:enter (fn [state] (xiana/ok (assoc-in state [:response :enter] true)))
   :leave (fn [state] (xiana/ok (assoc-in state [:response :leave] true)))})

(def test-override
  {:enter (fn [state]
            (xiana/ok
              (assoc-in state [:response :headers "override-in"] "true")))
   :leave (fn [state]
            (xiana/ok
              (assoc-in state [:response :headers "override-out"] "true")))})

(defn single-entry
  [action-map]
  {:enter (fn [{{uri  :uri
                 body :body-params} :request
                :as                 state}]
            (if (= uri "/action")
              (let [keywords (->> body
                                  ((juxt :resource :action))
                                  (map keyword))
                    action (get action-map keywords)]
                (xiana/ok (cond-> (assoc state
                                    :acl/access-map {:resource  (:resource body)
                                                     :privilege (:action body)})
                            (:id body) (assoc-in [:request :query-params :id] (:id body))
                            action (assoc-in [:request-data :action] action))))
              (xiana/ok state)))})

(def session-diff
  {:leave (fn [state]
            (xiana/ok state))})


