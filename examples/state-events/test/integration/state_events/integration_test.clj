(ns integration.state-events.integration-test
  (:require
    [clj-http.client :as client]
    [clojure.test :refer :all]
    [jsonista.core :as json]
    [state-events-fixture :as fixture])
  (:import
    (java.util
      UUID)))

(use-fixtures :once fixture/std-system-fixture)

(deftest new-event
  (let [resource-uuid (UUID/randomUUID)
        response (client/request
                   {:method       :put
                    :url          "http://localhost:3333/person"
                    :accept       :json
                    :content-type :json
                    :form-params  {:id       resource-uuid
                                   :action   "create"
                                   :resource "persons"}})]
    (is (= 200 (:status response)))))

(deftest modify-event
  (let [resource-uuid (UUID/randomUUID)
        create (client/request
                 {:method       :put
                  :url          "http://localhost:3333/person"
                  :accept       :json
                  :content-type :json
                  :form-params  {:id       (str resource-uuid)
                                 :action   "create"
                                 :resource "persons"}})
        modify (update (client/request {:method :post, :form-params {:email "Doe@john.it", :first-name "John", :id (str resource-uuid), :resource "persons", :action "modify"}, :url "http://localhost:3333/person", :content-type :json, :accept :json}) :body json/read-value json/keyword-keys-object-mapper)]
    (is (= 200 (:status create)))
    (is (= 200 (:status modify)))
    (is (= {:email      "Doe@john.it",
            :first-name "John",
            :id         (str resource-uuid),
            :resource   "persons"}
           (get-in modify [:body :data :events/payload])))))

(deftest cannot-create-already-existing-resource
  (let [resource-uuid (UUID/randomUUID)
        create (client/request
                 {:method       :put
                  :url          "http://localhost:3333/person"
                  :accept       :json
                  :content-type :json
                  :form-params  {:id       (str resource-uuid)
                                 :action   "create"
                                 :resource "persons"}})
        modify (update (client/request {:method :put, :form-params {:id (str resource-uuid), :resource "persons", :action "create"}, :url "http://localhost:3333/person", :content-type :json, :throw-exceptions false, :accept :json}) :body json/read-value json/keyword-keys-object-mapper)]
    (is (= 200 (:status create)))
    (is (= 403 (:status modify)))
    (is (= {:error       "Resource already exists"
            :resource    "persons"
            :resource-id (str resource-uuid)}
           (get-in modify [:body])))))
