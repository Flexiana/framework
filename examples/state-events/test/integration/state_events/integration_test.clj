(ns integration.state-events.integration-test
  (:require
    [clojure.test :refer :all]
    [jsonista.core :as json]
    [org.httpkit.client :as http]
    [state-events-fixture :as fixture])
  (:import
    (java.util
      UUID)))

(use-fixtures :once (partial fixture/std-system-fixture {}))

(deftest new-event
  (let [resource-uuid (UUID/fromString "68849768-fc9f-4602-814e-8b6cbeedd4b3")
        response @(http/request
                    {:method      :put
                     :url         "http://localhost:3333/person"
                     :as          :auto
                     :form-params {:id       resource-uuid
                                   :action   :create
                                   :resource :persons}})]
    (is (= 200 (:status response)))))

(deftest modify-event
  (let [resource-uuid (UUID/fromString "68849768-fc9f-4602-814e-8b6cbeedd4b3")
        create @(http/request
                  {:method      :put
                   :url         "http://localhost:3333/person"
                   :as          :auto
                   :form-params {:id       resource-uuid
                                 :action   :create
                                 :resource :persons}})
        modify (-> @(http/request
                      {:method      :post
                       :url         "http://localhost:3333/person"
                       :as          :text
                       :query-params {:email      "Doe@john.it"
                                      :first-name "John"
                                      :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3"
                                      :resource   "persons"}})

                   (update :body json/read-value json/keyword-keys-object-mapper))]
    (is (= 200 (:status create)))
    (is (= 200 (:status modify)))
    (is (= {:email      "Doe@john.it",
            :first-name "John",
            :id         "68849768-fc9f-4602-814e-8b6cbeedd4b3",
            :resource   "persons"}
           (get-in modify [:body :data :events/payload])))))
