(ns framework.interceptor.muuntaja
  "Prepare default munntaja instance."
  (:require
    [clojure.string]
    [clojure.data.xml :as xml]
    [muuntaja.core]
    [muuntaja.format.core]
    [muuntaja.interceptor]
    [muuntaja.format.json :as json-format]))

(defn xml-encoder
  "XML encoder."
  [_]
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

;; muuntaja instance
(defonce instance
  (muuntaja.core/create
    (-> muuntaja.core/default-options
        (assoc-in [:formats "application/upper-json"]
          {:decoder [json-format/decoder]
           :encoder [json-format/encoder
                     {:encode-key-fn (comp clojure.string/upper-case name)}]})
        (assoc-in [:formats "application/xml"] {:encoder [xml-encoder]})
        (assoc-in [:formats "application/json" :decoder-opts :bigdecimals] true)
        (assoc-in [:formats "application/json" :encoder-opts :date-format] "yyyy-MM-dd"))))

;; muuntaja default format interceptor
(defonce interceptor
  (muuntaja.interceptor/format-interceptor instance))
