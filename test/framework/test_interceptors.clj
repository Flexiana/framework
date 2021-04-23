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

(defn alt-acl
  ([]
   (alt-acl {}))
  ([m]
   :enter (fn [{acl :acl/access-map :as state}]
            (acl/is-allowed state (merge m acl)))
   :leave (fn [{query                 :query
                {{user-id :id} :user} :session-data
                owner-fn              :owner-fn
                :as                   state}]
            (xiana/ok (if owner-fn
                        (assoc state :query (owner-fn query user-id))
                        state)))))

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


