(ns cli-chat.interceptors
  (:require
    [xiana.core :as xiana]))

(def sample-cli-chat-controller-interceptor
  {:enter (fn [{request :request {:keys [handler controller match]} :request-data :as state}]
            state)
   :leave (fn [{response :response :as state}]
            state)})
