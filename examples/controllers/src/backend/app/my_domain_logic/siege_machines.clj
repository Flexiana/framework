(ns my-domain-logic.siege-machines)

(def fetch-whole-db
  (constantly
    {:siege-machine [{:id 1 :name :trebuchet}
                     {:id 2 :name :battering-ram :created #inst"2021-03-05T10"}
                     {:id 3 :name "puppet on strings"}]}))

(defn get-by-id
  [ctx]
  (let [data (fetch-whole-db (get-in ctx [:deps :db]))
        id (get-in ctx [:request :params :path :mydomain/id])
        response (->> data
                      :siege-machine
                      (filter #(= id (:id %)))
                      first)]
    (if response
      (assoc ctx :response {:status 200
                            :body   response})
      (throw (ex-info "Not found" {:status 404
                                   :body   "Not found"})))))
