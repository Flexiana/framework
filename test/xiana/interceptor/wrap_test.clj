(ns xiana.interceptor.wrap-test
  (:require
    [clojure.test :refer :all]
    [muuntaja.middleware :as middleware]
    [xiana.interceptor.wrap :as wrap])
  (:import
    (java.io
      ByteArrayInputStream)))

(def sample-state
  {:request  {:ssl-client-cert    nil,
              :protocol           "HTTP/1.1,"
              :remote-addr        "0:0:0:0:0:0:0:1",
              :params             {},
              :body-params        {:content "Update second time"},
              :headers            {:postman-token   "c78a4c40-8761-4301-ab70-64878ebd72f3",
                                   :user-agent      "PostmanRuntime / 7 .26.8",
                                   :authorization   "611d7f8a-456d-4f3c-802d-4d869dcd89bf",
                                   :host            "localhost:3333",
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
                        :data
                        {:acl     {:posts :own}
                         :db-data
                         {:posts [{:posts/id            #uuid "aeaf426b-bc79-4f19-9caf-33225f6a2679",
                                   :posts/user_id       #uuid "611d7f8a-456d-4f3c-802d-4d869dcd89bf",
                                   :posts/creation_time #inst "2021-04-06T07:57:15.162324000-00:00",
                                   :posts/content       "Update second time"}]}}}}})

;; define middleware mmuntaja interceptor
(def middleware-interceptor
  (-> {}
      (wrap/middleware->enter middleware/wrap-format-request)
      (wrap/middleware->leave middleware/wrap-format-response)))

(deftest contains-wrap-interceptor
  (let [interceptor (wrap/interceptor {:enter identity
                                       :leave identity
                                       :error identity})
        state {:request {:uri "/"}}
        enter ((:enter interceptor) state)
        leave ((:leave interceptor) state)
        error ((:error interceptor) state)]
    (is (and
          (= state (:right enter))
          (= state (:right leave))
          (= state (:left  error))))))

(deftest contains-midleware-enter
  (let [enter (-> (wrap/middleware->enter middleware/wrap-format-request)
                  (:enter))
        result (enter sample-state)]
    ;; verify middleware identity
    (is (= (:right result) sample-state))))

(deftest contains-midleware-leave
  (let [leave (->
                (wrap/middleware->leave middleware/wrap-format-request)
                (:leave))]
    (is (function? leave))))

(deftest contains-middleware-formated-response-body
  (is (= ByteArrayInputStream
         (-> ((:leave middleware-interceptor) sample-state)
             :right
             :response
             :body
             type))))
