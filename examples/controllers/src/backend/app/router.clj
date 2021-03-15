;TODO rename to routes
(ns router
  (:require
    [com.stuartsierra.component :as component]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [framework.components.app.core :as xiana.app]
    ;TODO do we want to require every part of domain logic here?
    [my-domain-logic.siege-machines :as mydomain.siege-machines]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [ring.middleware.params :as params]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.ring.middleware.muuntaja :as rm]
    [muuntaja.format.json :as json-format]
    [clojure.data.xml :as xml]
    [reitit.coercion.spec :as rcs]
    [reitit.coercion.malli :as rcm]
    [malli.core :as m]
    [malli.registry :as mr]
    [clojure.spec.alpha :as s]))

(def registry (merge
                (m/default-schemas)
                (malli.util/schemas)
                {:mydomain/SiegeMachine [:map
                                         [:id int?]
                                         [:name keyword?]
                                         [:range {:optional true} int?]
                                         [:created {:optional true} inst?]]
                 :mydomain/Infantry [:map
                                     [:id int?]
                                     [:name keyword?]
                                     [:attack {:optional true} int?]]}))

(mr/set-default-registry! registry)

(m/validate :mydomain/SiegeMachine {:id 1 :name :asd} {:registry registry})
(m/validate :mydomain/SiegeMachine {:id 1 :name :asd})


(extend-protocol xml/EventGeneration
  clojure.lang.Keyword
  (gen-event [s]
    (clojure.data.xml.Event. :chars nil nil (name s)))
  (next-events [_ next-items]
    next-items))

(defn xml-encoder [_options]
  (let [helper #(xml/emit-str
                  (mapv (fn make-node [[f s]]
                          (if (map? s)
                            (xml/element f {} (map make-node (seq s)))
                            (xml/element f {} s)))
                        (seq %)))]
    (reify
      muuntaja.format.core/EncodeToBytes
      (encode-to-bytes [_ data charset]
        (.getBytes ^String (helper data ) ^String charset)))))

(def minun-muuntajani
  (muuntaja.core/create
    (-> muuntaja.core/default-options
        (assoc-in [:formats "application/upper-json"]
                  {;:decoder [json-format/decoder]
                   :encoder [json-format/encoder {:encode-key-fn (comp clojure.string/upper-case name)}]})
        (assoc-in [:formats "application/xml"] {:encoder [xml-encoder]})
        (assoc-in [:formats "application/json" :decoder-opts :bigdecimals] true)
        (assoc-in [:formats "application/json" :encoder-opts :date-format] "yyyy-MM-dd"))))

(def routes
  [["/" {:controller index/index}]
   ["/re-frame" {:controller re-frame/index}]
   ["" {:controller xiana.app/default-controller
        :coercion (rcm/create {:registry registry})
        :muuntaja minun-muuntajani
        :middleware [parameters/parameters-middleware
                     rm/format-middleware
                     rrc/coerce-request-middleware
                     rrc/coerce-response-middleware]}
    ["/api/siege-machines/{mydomain/id}" {:get mydomain.siege-machines/get-by-id
                                          :parameters {:path [:map [:mydomain/id int?]]}
                                          :responses {200 {:body :mydomain/SiegeMachine}}}]
                                          ;:responses {200 {:body (m/schema :mydomain/SiegeMachine {:registry registry})}}}]
                                          ;:responses {200 {:body [:map [:id int?] [:name keyword?]]}}}]
    ;["/api/infantry/*" (schema->router :mydomain/Infantry)] ; TODO do we prefer dynamic per-model nested routers?
    ;["/api/*" (registry->router :mydomain)]
    ["/api/villagers/{mydomain/id}/tasklist"]]
   ;["/api/*" {:controller rest/ctrl}]
   ["/assets/*" (ring/create-resource-handler)]])
