(ns framework.route.core
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [xiana.commons :refer [deep-merge rename-key]]
    [xiana.core :as xiana]))

(defn not-found
  []
  (throw (ex-info "Not found"
                  {:status 404
                   :body "Not found"})))

(defn not-valid
  []
  (throw (ex-info "Request coercion failed"
                  {:status 400
                   :body   "Request coercion failed"})))

(defn uri->seq
  [uri]
  (when uri (re-seq #"\/[\-a-zA-Z0-9._~!$&'()*+,;=:@]*" uri)))

(defn extract
  [uri]
  (->> uri
       (re-find #"\/([\-a-zA-Z0-9._~!$&'()*+,;=:@\*]*)")
       second
       edn/read-string))

(defn uri->regex
  [uri]
  (re-pattern
    (apply str
           (concat
             (map
               (fn [v] (if (or (keyword? (extract v))
                               (= "/*" v))
                         "/([-a-zA-Z0-9._~!$&'()*+,;=:@]*)" v))
               (uri->seq uri))
             ["$"]))))

(defn uri->keys
  [uri]
  (reduce
    (fn [acc v]
      (if (or (keyword? (extract v))
              (= "/*" v))
        (conj acc v)
        acc))
    [] (uri->seq uri)))

(defn uri-match
  [[route act] uri]
  (let [m (re-find (uri->regex route) uri)
        ks (seq (uri->keys route))]
    (cond
      (nil? m) nil
      (nil? ks) [act {}]
      :else [act (into {} (map (fn [k v]
                                 [(extract k) v])
                               ks (rest m)))])))

(defn prepare
  "Preparing routes. Collecting the paths and actions from definition"
  [routes]
  (reduce
    (fn [acc route]
      (let [[uri act & sub-routes] route
            uri (str/replace uri #"\{([a-z]+)\/([a-z]+)\}" ":$1--$2")]
        (cond
          (empty? sub-routes) (conj acc [uri act sub-routes])
          :else (let [sr (mapv (fn [[u a & s]]
                                 (concat [(str uri u) (concat act a)] s)) sub-routes)]
                  (concat acc [[uri act]] (prepare sr))))))
    [] routes))

(defn unpack-var
  [var]
  (if (var? var) @var var))

(defn deep-deref-vars
  "De-referencing vars all deep in a vector"
  [s]
  (reduce (fn [acc v]
            (cond
              (var? v) (conj acc @v)
              (vector? v) (conj acc (deep-deref-vars v))
              :else (conj acc v)))
          [] s))

(defn deep-ref-vars
  "Replace all functions with var-refs all deep in a vector"
  [s]
  (reduce (fn [acc v]
            (cond
              (fn? v) (conj acc (try (var-get v)
                                     (catch Exception _ v)))
              (vector? v) (conj acc (deep-ref-vars v))
              :else (conj acc v)))
          [] s))

(defn reset
  "Convert route functions to references"
  [routes]
  (update routes :routes deep-ref-vars))

(defn name-spacer [m]
  (let [ks (map #(apply keyword (str/split (name %) #"--")) (keys m))
        v (vals m)]
    (zipmap ks v)))

(defn coerce!
  [validator params]
  (when-not (empty? validator)
    (if (and (every? validator (keys params))
             (every? params (keys validator))
             (every? true? (map (fn [[k v]]
                                  ((get validator k) (try (edn/read-string v)
                                                          (catch Exception _ v)))) params)))
      (zipmap (keys params) (map #(try (edn/read-string %) (catch Exception _ %)) (vals params)))
      (not-valid))))

(defn routing [matching-routes {:keys [request-method websocket?] :as req}]
  (let [[act params] (first matching-routes)
        params (name-spacer params)
        acts (when-not (fn? act) (into {} (deep-deref-vars act)))
        validator-map (merge {} (-> acts :parameters :path second))
        params (or (coerce! validator-map params) params)]
    (cond
      (fn? act) {:response (act req)}
      (empty? act) (not-found)
      :else (let [action (get acts request-method acts)
                  action (if websocket? (rename-key action :ws-action :action) action)]
              {:request-data (assoc action :match {:data action :path-params params})
               :request      {:params (assoc params :path params)}}))))

(defn router
  "Matches uri and method with defined routes. Returns defined subs"
  [routes]
  ;; prepared routes are grouped via number of path elements for faster resolution
  (let [length-grouped-routes (group-by #(-> % first uri->seq count) (prepare (unpack-var routes)))]
    (fn router**
      [{req :request :as state}]
      (xiana/ok (let [uri (:uri req)
                      matching-routes (->> uri uri->seq count
                                           (get length-grouped-routes)
                                           (map #(uri-match % uri))
                                           (remove nil?))
                      new-state (case (count matching-routes)
                                  0 (not-found)
                                  1 (routing matching-routes req)
                                  (throw (ex-info "Invalid routing"
                                                  {:status 500
                                                   :body "Invalid routing configuration"})))]
                  (deep-merge state new-state))))))
