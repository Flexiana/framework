(ns controllers.rest
  (:require
    [jsonista.core :as json]
    [clojure.data.xml :as xml]
    [xiana.core :as xiana]))

(extend-protocol xml/EventGeneration
  clojure.lang.Keyword
  (gen-event [s]
    (clojure.data.xml.Event. :chars nil nil (name s)))
  (next-events [_ next-items]
    next-items))

(defn negotiate-content-type
  [state]
  (let [content-type (-> state :http-request :headers (get "accept"))
        {{:keys [body status]} :response} state
        formatter (case content-type
                    "application/json" json/write-value-as-string
                    "application/xml"
                    #(xml/emit-str
                       (mapv (fn make-node [[f s]]
                               (if (map? s)
                                 (xml/element f {} (map make-node (seq s)))
                                 (xml/element f {} s)))
                         (seq %))))]
    (xiana/ok
      (assoc state
        :response
        {:status  status
         :headers {"Content-Type" content-type}
         :body    (formatter body)}))))

(defn require-logged-in
  [{req :http-request :as state}]
  (if-let [authorization (get-in req [:headers "authorization"])]
    (xiana/ok (assoc-in state [:session-data :authorization] authorization))
    (xiana/error (assoc state :response {:status 401 :body "Unauthorized"}))))

(defn action
  [{req :http-request {handler :handler} :request-data :as state}]
  (try
    (xiana/ok (assoc state :response (handler req)))
    (catch Exception e
      ;TODO I would like to see something logged here
      (xiana/error (assoc state :response {:status 500 :body "Internal Server error"})))))

(defn ctrl
  [state]
  (xiana/flow->
    state
    (require-logged-in)
    (action)))
    ;(negotiate-content-type)))
