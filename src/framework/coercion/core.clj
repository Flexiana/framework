(ns framework.coercion.core
  "Request and response validation by given malli rules.
   The rule can be defined at route definition,
   the schema can be defined with registry function.
   
   Path definition example:
   
   [\"/api/siege-machines/{mydomain/id}\" {:hander     ws/handler-fn
                                           :action     mydomain.siege-machines/get-by-id
                                           :parameters {:path [:map [:mydomain/id int?]]}
                                           :responses  {200 {:body :mydomain/SiegeMachine}}}]
                                           
   Registry example: 
    (registry {:mydomain/SiegeMachine [:map
                                         [:id int?]
                                         [:name keyword?]
                                         [:range {:optional true} int?]
                                         [:created {:optional true} inst?]]
                :mydomain/Infantry [:map
                                      [:id int?]
                                      [:name keyword?]
                                      [:attack {:optional true} int?]]})"
  (:require
    [malli.core :as m]
    [malli.error :as me]
    [malli.registry :as mr]
    [malli.util :as mu]
    [reitit.coercion :as coercion]
    [xiana.core :as xiana]))

(defn registry
  "Registers a given schema in malli"
  [schema]
  (mr/set-default-registry!
    (merge
      (m/default-schemas)
      (mu/schemas)
      schema)))

(def interceptor
  "On enter: validates request parameters
  On leave: validates response body
  on request error: responds {:status 400, :body \"Request coercion failed\"}
  on response error: responds {:status 400, :body \"Response validation failed\"}"
  {:enter (fn [state]
            (try (let [cc (-> (get-in state [:request-data :match])
                              coercion/coerce!)]
                   (xiana/ok (update-in state [:request :params] merge cc)))
                 (catch Exception e
                   (xiana/error (assoc state :response {:status 400 :body (ex-message e)})))))
   :leave (fn [{{:keys [:status :body]}  :response
                {method :request-method} :request
                :as                      state}]
            (let [schema (or (get-in state [:request-data :match :data method :responses status :body])
                             (get-in state [:request-data :match :data :responses status :body]))
                  valid? (some-> schema (m/validate body))]
              (if (false? valid?)
                (let [explain (m/explain schema body)
                      humanized (me/humanize explain)]
                  (xiana/error (assoc state :response {:status 400
                                                       :body {:explain explain
                                                              :humanized humanized}})))
                (xiana/ok state))))})

