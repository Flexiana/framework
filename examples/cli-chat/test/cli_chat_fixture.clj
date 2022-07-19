(ns cli-chat-fixture
  (:require
    [cli-chat.core :refer [->system]]
    [clj-test-containers.core :as tc]
    [xiana.config :as config]))

(defn docker-postgres!
  [{pg-config :xiana/postgresql :as config}]
  (let [{:keys [dbname user password image-name]} pg-config
        container (-> (tc/create
                        {:image-name    image-name
                         :exposed-ports [5432]
                         :env-vars      {"POSTGRES_DB"       dbname
                                         "POSTGRES_USER"     user
                                         "POSTGRES_PASSWORD" password}})
                      (tc/start!))
        port (get (:mapped-ports container) 5432)
        pg-config (assoc
                    pg-config
                    :port port
                    :embedded container
                    :subname (str "//localhost:" port "/" dbname))]
    (tc/wait {:wait-strategy :log
              :message       "accept connections"} (:container container))
    (assoc config :xiana/postgresql pg-config)))

(defn std-system-fixture
  [app-cfg f]
  (with-open [_ (-> (config/config)
                    (merge app-cfg)
                    docker-postgres!
                    ->system)]
    (f)))
