(ns integration.state-events.integration-test
  (:require
    [clojure.test :refer :all]
    [jsonista.core :as json]
    [org.httpkit.client :as http]
    [state-events-fixture :as fixture])
  (:import
    (java.util
      UUID)))

(use-fixtures :once fixture/std-system-fixture)

(deftest new-event
  (let [resource-uuid (UUID/randomUUID)
        response @(http/request
                    {:method      :put
                     :url         "http://localhost:3333/person"
                     :as          :auto
                     :form-params {:id       resource-uuid
                                   :action   :create
                                   :resource :persons}})]
    (is (= 200 (:status response)))))

(deftest modify-event
  (let [resource-uuid (UUID/randomUUID)
        create @(http/request
                  {:method       :put
                   :url          "http://localhost:3333/person"
                   :as           :auto
                   :query-params {:id       (str resource-uuid)
                                  :action   :create
                                  :resource :persons}})
        modify (-> @(http/request
                      {:method       :post
                       :url          "http://localhost:3333/person"
                       :as           :text
                       :query-params {:action :modify
                                      :email      "Doe@john.it"
                                      :first-name "John"
                                      :id         (str resource-uuid)
                                      :resource   :persons}})

                   (update :body json/read-value json/keyword-keys-object-mapper))]
    (is (= 200 (:status create)))
    (is (= 200 (:status modify)))
    (is (= {:email      "Doe@john.it",
            :first-name "John",
            :id         (str resource-uuid),
            :resource   ":persons"}
           (get-in modify [:body :data :events/payload])))))

(deftest cannot-create-already-existing-resource
  (let [resource-uuid (UUID/randomUUID)
        create @(http/request
                  {:method       :put
                   :url          "http://localhost:3333/person"
                   :as           :auto
                   :query-params {:id       (str resource-uuid)
                                  :action   :create
                                  :resource :persons}})
        modify (-> @(http/request
                      {:method       :put
                       :url          "http://localhost:3333/person"
                       :as           :auto
                       :query-params {:id       (str resource-uuid)
                                      :action   :create
                                      :resource :persons}})
                   (update :body json/read-value json/keyword-keys-object-mapper))]
    (is (= 200 (:status create)))
    (is (= 403 (:status modify)))
    (is (= {:error       "Resource already exists"
            :resource    ":persons"
            :resource-id (str resource-uuid)}
           (get-in modify [:body])))))
