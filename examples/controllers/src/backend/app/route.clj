(ns route
  (:require
    [clojure.data.xml :as xml]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [malli.core :as m]
    [malli.registry :as mr]
    [malli.util :as mu]
    [muuntaja.format.core]
    [my-domain-logic.siege-machines :as mydomain.siege-machines]
    [reitit.coercion.malli :as rcm]
    [reitit.ring :as ring]
    [xiana.handler :as handler])
  (:import
    (clojure.data.xml
      Event)
    (clojure.lang
      Keyword)))

(def registry
  (merge
    (m/default-schemas)
    (mu/schemas)
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
  Keyword
  (gen-event [s]
    (Event. :chars nil nil (name s)))
  (next-events [_ next-items]
    next-items))

(defn xml-encoder
  [_options]
  (let [helper #(xml/emit-str
                  (mapv (fn make-node
                          [[f s]]
                          (if (map? s)
                            (xml/element f {} (map make-node (seq s)))
                            (xml/element f {} s)))
                        (seq %)))]
    (reify
      muuntaja.format.core/EncodeToBytes
      (encode-to-bytes [_ data charset]
        (.getBytes ^String (helper data) ^String charset)))))

(def routes
  [["/" {:action index/index}]
   ["/re-frame" {:action re-frame/index}]
   ["" {:coercion   (rcm/create {:registry registry})}
    ["/api/siege-machines/{mydomain/id}" {:hander     handler/handler-fn
                                          :action     mydomain.siege-machines/get-by-id
                                          :parameters {:path [:map [:mydomain/id int?]]}
                                          :responses  {200 {:body :mydomain/SiegeMachine}}}]]
   ["/assets/*" (ring/create-resource-handler)]])
