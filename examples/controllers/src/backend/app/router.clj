;TODO rename to routes
(ns router
  (:require
    [clojure.data.xml :as xml]
    [clojure.string :as str]
    [controllers.index :as index]
    [controllers.re-frame :as re-frame]
    [framework.route.helpers :as helpers]
    [framework.webserver.core :as ws]
    [malli.core :as m]
    [malli.registry :as mr]
    [malli.util :as mu]
    [muuntaja.core :as munc]
    [muuntaja.format.json :as json-format]
    [my-domain-logic.siege-machines :as mydomain.siege-machines]
    [reitit.coercion.malli :as rcm]
    [reitit.ring :as ring])
  (:import
    (clojure.data.xml
      Event)
    (clojure.lang
      Keyword)
    (muuntaja.format.core
      EncodeToBytes)))

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
      EncodeToBytes
      (encode-to-bytes [_ data charset]
        (.getBytes ^String (helper data) ^String charset)))))

(def minun-muuntajani
  (munc/create
    (-> muuntaja.core/default-options
        (assoc-in [:formats "application/upper-json"]
                  {;:decoder [json-format/decoder]
                   :encoder [json-format/encoder {:encode-key-fn (comp str/upper-case name)}]})
        (assoc-in [:formats "application/xml"] {:encoder [xml-encoder]})
        (assoc-in [:formats "application/json" :decoder-opts :bigdecimals] true)
        (assoc-in [:formats "application/json" :encoder-opts :date-format] "yyyy-MM-dd"))))

(def routes
  [["/" {:action index/index}]
   ["/re-frame" {:action re-frame/index}]
   ["" {:action     helpers/action
        :coercion   (rcm/create {:registry registry})
        :muuntaja   minun-muuntajani}
    ["/api/siege-machines/{mydomain/id}" {:hander     ws/handler-fn
                                          :action     mydomain.siege-machines/get-by-id
                                          :parameters {:path [:map [:mydomain/id int?]]}
                                          :responses  {200 {:body :mydomain/SiegeMachine}}}]]
   ["/assets/*" (ring/create-resource-handler)]])
