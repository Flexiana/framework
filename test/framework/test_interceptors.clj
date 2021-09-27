(ns framework.test-interceptors
  (:require
    [xiana.core :as xiana]))

(def test-interceptor
  "test interceptor to see interceptor injection"
  {:enter (fn [state] (xiana/ok (assoc-in state [:response :enter] true)))
   :leave (fn [state] (xiana/ok (assoc-in state [:response :leave] true)))})

(def test-override
  "test interceptor to see interceptors overriding"
  {:enter (fn [state]
            (xiana/ok
              (assoc-in state [:response :headers "override-in"] "true")))
   :leave (fn [state]
            (xiana/ok
              (assoc-in state [:response :headers "override-out"] "true")))})

(defn single-entry
  "POC for set up the flow/routing from interceptor for one-endpoint"
  [action-map url]
  {:enter (fn [{{uri  :uri
                 body :body-params} :request
                :as                 state}]
            (if (= uri url)
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

(def response-session
  "POC for injecting session into the response"
  {:leave (fn [state]
            (let [old-session (dissoc (get  state :session-data {}) :new-session)
                  body (get-in state [:response :body])
                  new-session (-> (assoc old-session :view-type (:view-type body))
                                  (merge (:data body)))]
              (xiana/ok (assoc-in state [:response :body] new-session))))})
