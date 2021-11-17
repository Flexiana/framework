(ns state-events.models.person)

(defn ->person
  [body]
  (select-keys body [:id :first_name :last_name :email :phone :city]))

(defn add [state]
  (let [uuid (-> state :request :params :uuid)]
    (assoc state :query {:insert-into :persons
                         :values [{:id uuid}]})))

(defn modify [state]
  (let [uuid (-> state :request :params :uuid)
        body (-> state :request :body)]
    (assoc state :query {:insert-into :persons
                         :values [(->person body)]
                         :where [:= :id uuid]})))

