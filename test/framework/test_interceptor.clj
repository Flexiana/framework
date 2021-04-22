(ns framework.test-interceptor
  (:require
    [framework.acl.core :as acl]
    [xiana.core :as xiana]))

(def test-interceptor
  {:enter (fn [state] (xiana/ok (assoc-in state [:response :enter] true)))
   :leave (fn [state] (xiana/ok (assoc-in state [:response :leave] true)))})

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

(defn session-exchange-pre-route
  [action-map views-map]
  {:enter (fn [{{uri  :uri
                 body :body-params} :request
                :as                 state}]
            (if (= uri "/session")
              (let [view (->> body
                              ((juxt :resource :action))
                              (map keyword)
                              views-map)
                    action (->> body
                                ((juxt :resource :action))
                                (map keyword)
                                action-map)]
                (xiana/ok (-> (assoc state
                                :view view
                                :acl/access-map {:resource  (:resource body)
                                                 :privilege (:action body)})
                              (assoc-in [:request-data :action] action))))
              (xiana/ok state)))})
