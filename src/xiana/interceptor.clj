(ns xiana.interceptor
  "Collection of useful interceptors"
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.walk :refer [keywordize-keys]]
    [ring.middleware.params :as middleware.params]
    [xiana.interceptor.muuntaja :as muuntaja]))

(def log
  "Log interceptor.

   - Enter: Print 'Enter:' followed by the complete state map.
   - Leave: Print 'Leave:' followed by the complete state map."
  {:enter (fn [state] (pprint ["Enter: " state]) state)
   :leave (fn [state] (pprint ["Leave: " state]) state)})

(def side-effect
  "Side-effect interceptor.

   - Enter: nil.
   - Leave: apply `:side-effect` state key to state if it exists."
  {:name ::side-effect
   :leave
   (fn [state]
     (if-let [f (get state :side-effect)]
       (f state)
       state))})

(def view
  "View interceptor

   - Enter: nil
   - Leave: apply `:view` state key to state if it exists and `:response` is absent."
  {:leave
   (fn [{:keys [view response] :as state}]
     (if (and (not (:body response)) view)
       (view state)
       state))})

;; Question: should be used in the early chain of interceptors?
(def params
  "Update the request map with parsed url-encoded parameters.
  Adds the following keys to the request map:

  `:query-params` - a map of parameters from the query string

  `:form-params`  - a map of parameters from the body

  `:params`       - a merged map of all types of parameter"
  {:name  ::params
   :enter (fn [state]
            (let [f #(keywordize-keys
                       ((middleware.params/wrap-params identity) %))]
              (update state :request f)))})

(defn message                                               ; TODO: remove, use logger
  "This interceptor creates a function that prints predefined message.

   - Enter: Print an arbitrary message.
   - Leave: Print an arbitrary message."
  [msg]
  {:enter (fn [state] (println msg) state)
   :leave (fn [state] (println msg) state)})

(defn muuntaja
  "Muuntaja encoder/decoder interceptor."
  ([] (muuntaja muuntaja/interceptor))
  ([interceptor] interceptor))

(defn- get-request? [{:keys [request-method]}]
  (= :get request-method))

(def prune-get-request-bodies
  "This interceptor removes bodies from GET requests on Enter."
  {:name  ::prune-get-bodies
   :enter (fn [{:keys [request] :as state}]
            (if (get-request? request)
              (update state :request dissoc :body :body-params)
              state))})
