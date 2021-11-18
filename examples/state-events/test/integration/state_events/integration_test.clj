(ns integration.state-events.integration-test
  (:require
    [clojure.test :refer :all]
    [jsonista.core :as json]
    [org.httpkit.client :as http]
    [state-events-fixture :as fixture]))

(use-fixtures :once (partial fixture/std-system-fixture {}))

(deftest new-event
  #_(let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
          cl (http/create-client)
          response @(http/PUT cl
                              "http://localhost:3333/person"
                              :body {:id       resource-uuid
                                     :action   :create
                                     :resource :persons})]
      (is (nil? response)))

  (let [resource-uuid "68849768-fc9f-4602-814e-8b6cbeedd4b3"
        response @(http/request
                    {:method      :put
                     :url         "http://localhost:3333/person"
                     :as :auto
                     :form-params {:id       resource-uuid
                                   :action   :create
                                   :resource :persons}})]
    (println response)

    (is (nil? (-> response)))))
