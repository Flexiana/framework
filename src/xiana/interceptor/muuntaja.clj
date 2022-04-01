(ns xiana.interceptor.muuntaja
  "Muuntaja interceptor encoder/decode.."
  (:require
    [clojure.data.xml :as xml]
    [clojure.string]
    [muuntaja.core]
    [muuntaja.format.core]
    [muuntaja.format.json :as json]
    [muuntaja.interceptor]))

(defn xml-encoder
  "XML encoder."
  [_]
  (let [helper-fn
        #(xml/emit-str
           (mapv
             (fn make-node
               [[f s]]
               (xml/element f {} (if (map? s) (map make-node (seq s)) s)))
             (seq %)))]
    ;; implement EncodeToBytes protocol
    (reify
      muuntaja.format.core/EncodeToBytes
      (encode-to-bytes
        [_ data charset]
        (.getBytes ^String (helper-fn data) ^String charset)))))

(def instance
  "Define muuntaja's default encoder/decoder instance."
  (muuntaja.core/create
    (-> muuntaja.core/default-options
        (assoc-in [:formats "application/upper-json"]
                  {:decoder [json/decoder]
                   :encoder [json/encoder
                             {:encode-key-fn
                              (comp clojure.string/upper-case name)}]})
        (assoc-in [:formats "application/xml"] {:encoder [xml-encoder]})
        (assoc-in [:formats "application/json" :decoder-opts :bigdecimals] true)
        (assoc-in [:formats "application/json" :encoder-opts :date-format]
                  "yyyy-MM-dd"))))

(def interceptor
  "Define muuntaja's default interceptor."
  (muuntaja.interceptor/format-interceptor instance))
