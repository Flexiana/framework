(ns my-domain-logic.siege-machines
  (:require
    [ring.util.response :as ring]))

(def fetch-whole-db
  (constantly
    {:siege-machine [{:id 1 :name :trebuchet}
                     {:id 2 :name :battering-ram :created #inst"2021-03-05T10"}
                     {:id 3 :name "puppet on strings"}]}))

(defn get-by-id
  [{{{id :mydomain/id} :path} :parameters :as req}]
  (let [data (fetch-whole-db (:db req))]
    (ring/response
      (->> data
           :siege-machine
           (filter #(= id (:id %)))
           first))))
