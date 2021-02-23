(ns my-domain-logic.siege-machines)

(def fetch-whole-db
  (constantly
    {:siege-machine {1 :trebuchet
                     2 :battering-ram
                     3 "puppet on strings"}}))

;(defn get-by-id [{{id :id} :path-params :as req}]
(defn get-by-id [{{{id :id} :path} :parameters :as req}]
  (let [data (fetch-whole-db (:db req))]
    ;TODO use ring/response
    {:status 200
     :body (get-in data [:siege-machine id])}))

