(ns xiana.interceptor.muuntaja-test
  (:require
    [clojure.data.xml :as xml]
    [clojure.test :refer :all]
    [muuntaja.format.core :as format]
    [xiana.core :as xiana]
    [xiana.interceptor.muuntaja :as muuntaja]))

(def data-sample [["note" "anything" "note"]])

(deftest contains-default-xlm
  (let [instance   (muuntaja/xml-encoder '_)
        byte-array (format/encode-to-bytes instance {} "utf-8")
        XLM-string (apply str (map #(char (bit-and % 255)) byte-array))
        expected   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"]
    ;; verify if response is equal to the expected
    (is (= XLM-string expected))))

(deftest enconde-arbitrary-xml
  (let [instance   (muuntaja/xml-encoder '_)
        byte-array (format/encode-to-bytes instance data-sample "utf-8")
        XLM-string (apply str (map #(char (bit-and % 255)) byte-array))
        expected   "<?xml version=\"1.0\" encoding=\"UTF-8\"?><note>anything</note>"]
    ;; verify if response is equal to the expected
    (is (= XLM-string expected))))
