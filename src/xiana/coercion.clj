(ns xiana.coercion
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
    [malli.registry :as mr]
    [malli.util :as mu]
    [reitit.coercion :as coercion]))

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
                   (update-in state [:request :params] merge cc))
                 (catch Exception _
                   (throw (ex-info "Request coercion failed" {:status 400 :body   "Request coercion failed"})))))
   :leave (fn [{{:keys [:status :body]} :response
                :as                     state}]
            (let [schema (get-in state [:request-data :match :data :responses status :body])]
              (cond (and schema body (m/validate schema body)) state
                    (and schema body) (throw (ex-info "Response validation failed"
                                                      {:status 400
                                                       :body   "Response validation failed"}))
                    :else state)))})
