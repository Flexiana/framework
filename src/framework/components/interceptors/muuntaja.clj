(ns framework.components.interceptors.muuntaja
  (:require
    [clojure.data.xml :as xml]
    [clojure.string]
    [muuntaja.core]
    [muuntaja.format.core]
    [muuntaja.format.json :as json-format]
    [muuntaja.interceptor]))

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

(def minun-muuntajani
  (muuntaja.core/create
    (-> muuntaja.core/default-options
        (assoc-in [:formats "application/upper-json"]
          {:decoder [json-format/decoder]
           :encoder [json-format/encoder {:encode-key-fn (comp clojure.string/upper-case name)}]})
        (assoc-in [:formats "application/xml"] {:encoder [xml-encoder]})
        (assoc-in [:formats "application/json" :decoder-opts :bigdecimals] true)
        (assoc-in [:formats "application/json" :encoder-opts :date-format] "yyyy-MM-dd"))))

(def muun-instance (muuntaja.interceptor/format-interceptor minun-muuntajani))
