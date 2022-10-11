(ns website-gen
  (:require
    [clojure.string :as str]
    [markdown.core :as md])
  (:import
    (java.io
      File)))

(defn parse-args
  [args]
  (apply hash-map
         (reduce (fn [acc a]
                   (vec
                     (cond
                       (.startsWith a "--")
                       (conj acc (keyword (str/replace-first a #"--" "")))

                       (keyword? (last acc))
                       (conj acc a)

                       :else
                       (let [head (vec (butlast acc))
                             tail (if (string? (last acc))
                                    [(last acc)]
                                    (last acc))]
                         (concat head [(conj tail a)])))))
                 []
                 args)))

(defn header [html]
  (second (re-find #"\<h1.*>(.*)\<\/h1>" html)))

(defn read-file [^File f]
  (let [content (slurp f)
        html    (md/md-to-html-string content :heading-anchors true)
        corrected (str/replace html #"<li><a href='#([a-z\-]+)"
                               (fn [[x _]]
                                 (str/replace x #"-" "_")))
        header  (header html)]
    {:source-name (.getName f)
     :markdown    content
     :html        corrected
     :out-name    (str (str/replace header #"[^a-zA-Z]" "-") ".html")
     :header      header}))

(defn read-files [files]
  (->> files
       (filter #(.isFile %))
       (filter #(or (.endsWith (.getName %) ".md")
                    (.endsWith (.getName %) ".markdown")))
       (map read-file)))

(defn read-dir [source]
  (when source (.listFiles (File. source))))

(defn read-sources [sources]
  (when sources (map #(File. %) sources)))

(defn write [target files]
  (doseq [f files]
    (spit (str/join "/" [target (:out-name f)]) (:html f))))

(defn -main [& args]
  (let [{:keys [:sources :source-dir :target]} (parse-args args)
        files      (read-files (concat (read-dir source-dir) (read-sources sources)))
        target-dir (File. target)]
    (when (.isDirectory target-dir)
      (.delete target-dir))
    (.mkdir target-dir)
    (write target files)))

(comment
  (parse-args ["--source-dir" "doc"
               "--target" "www/target"
               "--sources"
               "doc/welcome.md"
               "doc/conventions.md"
               "doc/tutorials.md"
               "doc/how-to.md"
               "doc/contribution.md"
               "doc/getting-started.md"])
  (read-files [(File. "doc/tutorials.md")])
  (-main "--source-dir" "doc" "--target" "www/target" "--sources" "doc/welcome.md"
         "doc/conventions.md"
         "doc/tutorials.md"
         "doc/how-to.md"
         "doc/contribution.md"
         "doc/getting-started.md"))
