(ns framework.components.interceptors.wrap-test
  (:require
    [clojure.test :refer :all]
    [framework.components.interceptors.wrap :refer [middleware->leave
                                                    middleware->enter]]
    [muuntaja.middleware :as m])
  (:import
    (java.io
      ByteArrayInputStream)))

(def to-test
  (-> {}
      (middleware->enter m/wrap-format-request)
      (middleware->leave m/wrap-format-response)))

(def req-res
  {:request  {:ssl-client-cert    nil,
              :protocol           "HTTP/1.1,"
              :remote-addr        "0:0:0:0:0:0:0:1",
              :params             {},
              :body-params        {:content "Update second time"},
              :headers            {:postman-token   "c78a4c40-8761-4301-ab70-64878ebd72f3",
                                   :user-agent      "PostmanRuntime / 7 .26.8",
                                   :authorization   "611d7f8a-456d-4f3c-802d-4d869dcd89bf",
                                   :host            "localhost:3000",
                                   :content-length  34,
                                   :accept-encoding "gzip",
                                   "deflate",
                                   "br",
                                   :content-type    "application/json",
                                   :connection      "keep-alive",
                                   :accept          "application/json"},
              :server-port        3000,
              :muuntaja/request   {:format     "application/json",
                                   :charset    "utf-8",
                                   :raw-format "application/json"},
              :content-length     34,
              :form-params        {},
              :query-params       {},
              :content-type       "application/json",
              :character-encoding "UTF-8",
              :uri                "/posts",
              :server-name        "localhost",
              :query-string       nil,
              :muuntaja/response  {:format     "application/json",
                                   :charset    "utf-8",
                                   :raw-format "application/json"},
              :scheme             :http,
              :request-method     :put},
   :response {:status  200,
              :headers {"Content-type" "Application/json",
                        "Session-id"   "62e665fe-d240-4f6a-af84-d3d734fa302a"},
              :body    {:view-type "All posts",
                        :data      {:acl     {:posts :own}
                                    :db-data {:posts [{:posts/id            #uuid "aeaf426b-bc79-4f19-9caf-33225f6a2679",
                                                       :posts/user_id       #uuid "611d7f8a-456d-4f3c-802d-4d869dcd89bf",
                                                       :posts/creation_time #inst "2021-04-06T07:57:15.162324000-00:00",
                                                       :posts/content       "Update second time"}]}}}}})

(deftest middleware->leave-test
  (is (= ByteArrayInputStream
         (-> ((:leave to-test) req-res)
             :right
             :response
             :body
             type))))

