# How to

- [Login implementation](#login-implementation)
- [Logout implementation](#logout-implementation)
- [Use coercion](#use-coercion)

## Login implementation

Xiana framework don't have login and logout functions because all application can be different on user management. The
session interceptor can validate a request only if the session is already exists in session storage. So to login a user,
you need to add its session to the storage. All session should have unique, and valid UUID as session-id, and this ID
should be part of active session too. The active session goes to the `(-> state :sessoin-data)`. It's loaded and stored
in session storage before/after reaching the action. But if a referred session isn't in the storage, the execution flow
will be interrupted before the flow reaching the action function, and responses with:

```clojure
{:status 401
 :body   "Invalid or missing session"}
```

To implement login functionality, you must [skip](tutorials.md#interceptor-overriding) the session interceptor in route
definition, and make a session by hand:

```clojure
(let [;create
      session-id (UUID/randomUUID)]
  ;store a new session in session storage,
  (add! session-storage session-id {:session-id session-id})
  ;and be sure the session-id will be part of the response
  (xiana/ok (assoc-in state [:response :headers :session-id] (str session-id))))
```

If you want to use role based access control, you need to store the actual user in your session data. To get your user
from database, you will create a query in models/user namespace like this one:

```clojure
(defn fetch-query
  [state]
  (let [login (-> state :request :body-params :login)]
    (-> (select :*)
        (from :users)
        (where [:and
                :is_active
                [:or
                 [:= :email login]
                 [:= :username login]]]))))
```

and execute it with the `db-access` interceptor, which injects the result of query into the state. If you already have
this in the state, you can modify your session creation function:

```clojure
(let [;get user from database result
      user (-> state :response-data :db-data first)
      ;create session
      session-id (UUID/randomUUID)]
  ;store a new session in session storage,
  (add! session-storage session-id {:session-id session-id
                                    :user       user})
  ;and be sure the session-id will be part of the response
  (xiana/ok (assoc-in state [:response :headers :session-id] (str session-id))))
```

Oh man, you don't want to store the user's password in any kind of storage, are you?

```clojure
(let [;get user from database result
      user (-> state
               :response-data
               :db-data
               first
               ;remove password for session storage
               (dissoc :users/password))
      ;create session
      session-id (UUID/randomUUID)]
  ;store a new session in session storage,
  (add! session-storage session-id {:session-id session-id
                                    :user       user})
  ;and be sure the session-id will be part of the response
  (xiana/ok (assoc-in state [:response :headers :session-id] (str session-id))))
```

Okay, all we need to do here, is check if the credentials are correct, right? So put it all into an `if` statement.

```clojure
(if (valid-credentials?)
  (let [;get user from database result
        user (-> state
                 :response-data
                 :db-data
                 first
                 ;remove password for session storage
                 (dissoc :users/password))
        ;create session
        session-id (UUID/randomUUID)]
    ;store a new session in session storage,
    (add! session-storage session-id {:session-id session-id
                                      :user       user})
    ;and be sure the session-id will be part of the response
    (xiana/ok (assoc-in state [:response :headers :session-id] (str session-id))))
  (xiana/error (assoc state :response {:status 401
                                       :body   "Login failed"})))
```

Is Xiana provides you anything to check if the user password and the stored is equivalent? Sure, it is
in `framework.auth.hash`

```clojure
(defn- valid-credentials?
  "It checks whether the password provided by the user matches the encrypted one that is stored in the database."
  [state]
  (let [user-provided-pass (-> state :request :body-params :password)
        db-stored-pass (-> state :response-data :db-data first :users/password)]
    (and user-provided-pass
         db-stored-pass
         (hash/check state user-provided-pass db-stored-pass))))
```

and all together:

```clojure
(defn- valid-credentials?
  "It checks whether the password provided by the user matches the encrypted one that is stored in the database."
  [state]
  (let [user-provided-pass (-> state :request :body-params :password)
        db-stored-pass (-> state :response-data :db-data first :users/password)]
    (and user-provided-pass
         db-stored-pass
         (hash/check state user-provided-pass db-stored-pass))))

(defn login
  [state]
  (if (valid-credentials? state)
    (let [;get user from database result
          user (-> state
                   :response-data
                   :db-data
                   first
                   ;remove password for session storage
                   (dissoc :users/password))
          ;create session
          session-id (UUID/randomUUID)]
      ;store a new session in session storage,
      (add! session-storage session-id {:session-id session-id
                                        :user       user})
      ;and be sure the session-id will be part of the response
      (xiana/ok (assoc-in state [:response :headers :session-id] (str session-id))))
    ;Ops something is missing, or passwords are not matching
    (xiana/error (assoc state :response {:status 401
                                         :body   "Login failed"}))))
```

Right. The login-logic is done, looks good. Where to put it?

Do you remember [side effect interceptor?](interceptors.md#side-effect) It's runs after we have the query result from
database, and before rendering the final response with [view interceptor.](interceptors.md#view) The place of the
function defined above, is there in the interceptor chain. How it goes there? Let see an [action](conventions.md#action)
.

```clojure
(defn action
  [state]
  (xiana/flow->
    (assoc state :side-effect side-effects/login)))
```

This is the place for inject the database query too:

```clojure
(defn action
  [state]
  (xiana/flow->
    (assoc state :side-effect side-effects/login
                 :query model/fetch-query)))
```

But some tiny thing is still missing. The definition of the response in all-ok case. A happy path response.

```clojure
(defn login-success
  [state]
  (let [id (-> state :response-data :db-data first :users/id)]
    (-> (assoc-in state [:response :body]
                  {:view-type "login"
                   :data      {:login   "succeed"
                               :user-id id}})
        (assoc-in [:response :status] 200)
        xiana/ok)))
```

And finally the [view](tutorials.md#view) is injected in the action function:

```clojure
(defn action
  [state]
  (xiana/flow->
    (assoc state :side-effect side-effects/login
                 :view view/login-success
                 :query model/fetch-query)))
```

## Logout implementation

To do a logout, much easier than the login. The `session-interceptor` do the half of the work, and if you have a running
session, then it will not complain. The only thing what you should do, is to remove the actual session from the `state`
and form session storage. Something like this:

```clojure
(defn logout
  [state]
  (let [session-store (get-in state [:deps :session-backend])
        session-id (get-in state [:session-data :session-id])]
    (session/delete! session-store session-id)
    (xiana/ok (dissoc state :session-data))))
```

Adding the `ok` response
```clojure
(defn logout-view
  [state]
  (-> (assoc-in state [:response :body] {:view-type "logout"
                                         :data      {:logout "succeed"}})
      (assoc-in [:response :status] 200)))
```
and using it:
```clojure
(defn logout
  [state]
  (let [session-store (get-in state [:deps :session-backend])
        session-id (get-in state [:session-data :session-id])]
    (session/delete! session-store session-id)
    (xiana/ok (-> (dissoc state :session-data)
                  (assoc :view views/logout-view)))))
```

## Use coercion

