# How to

- [Login implementation](#login-implementation)
- [Logout implementation](#logout-implementation)
- [Access and data ownership control](#access-and-data-ownership-control)
    - [Role set definition](#role-set-definition)
    - [Provide resource/action at routing](#provide-resourceaction-at-routing)
    - [Application start-up](#application-start-up)
    - [Access control](#access-control)
    - [Data ownership](#data-ownership)
    - [All together](#all-together)

## Login implementation

Xiana framework does not have login and logout functions because the application can differ on user management. The
session interceptor can validate a request only if the session already exists in session storage. So to log in a user,
you need to add its session to the storage. All sessions should have a unique, valid UUID as session-id and this ID
should be part of an active session. The active session goes to the `(-> state :session-data)`. It is loaded and stored
in session storage before/after reaching the action. But if a referred session is not in the storage, the execution flow
will be interrupted before the flow reaches the action function, and responds with:

```clojure
{:status 401
 :body   "Invalid or missing session"}
```

To implement login functionality, you must [skip](tutorials.md#interceptor-overriding) use the session interceptor in route
definition and make a session manually:

```clojure
(let [;create
      session-id (UUID/randomUUID)]
  ;store a new session in session storage,
  (add! session-storage session-id {:session-id session-id})
  ;Be sure the session-id will be part of the response
  (xiana/ok (assoc-in state [:response :headers :session-id] (str session-id))))
```

If you want to use role-based access control, you need to store the actual user in your session data. To get your user
from the database, you will create a query in the models/user namespace ex:

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

and execute it with the `db-access` interceptor, which injects the query result into the state. 
If you already have this in the state, you can modify your create session function:

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

Be sure to remove the user's password and any other sensitive information that will be stored:

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

Next, we check if the credentials are correct, so we use an `if` statement.

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

Xiana provides `framework.auth.hash` to check user credentials:

```clojure
(defn- valid-credentials?
  "It checks that the password provided by the user matches the encrypted password from the database."
  [state]
  (let [user-provided-pass (-> state :request :body-params :password)
        db-stored-pass (-> state :response-data :db-data first :users/password)]
    (and user-provided-pass
         db-stored-pass
         (hash/check state user-provided-pass db-stored-pass))))
```

The login logic is done, but where to place it?

Do you remember [side effect interceptor?](interceptors.md#side-effect) It's running after we have the query result from
the database, and before rendering the final response with [view interceptor.](interceptors.md#view) The place of the
function defined above is in the interceptor chain. How it goes there? Let see an [action](conventions.md#action)
.

```clojure
(defn action
  [state]
  (xiana/ok
    (assoc state :side-effect side-effects/login)))
```

This is the place for inject the database query too:

```clojure
(defn action
  [state]
  (xiana/ok
    (assoc state :side-effect side-effects/login
                 :query model/fetch-query)))
```

But some tiny thing is still missing. The definition of the response in the all-ok case. A happy path response.

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
  (xiana/ok
    (assoc state :side-effect side-effects/login
                 :view view/login-success
                 :query model/fetch-query)))
```

## Logout implementation

To do a logout is much easier than a login implementation. The `session-interceptor` does half of the work, and if you have a running
session, then it will not complain. The only thing you should do is to remove the actual session from the `state`
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
  (xiana/ok (-> (assoc-in state [:response :body]
                          {:view-type "logout"
                           :data      {:logout "succeed"}})
                (assoc-in [:response :status] 200))))
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

## Access and data ownership control

[RBAC](tutorials.md#role-based-access-and-data-ownership-control) is a handy way to restrict user actions on different
resources. It's a role-based access control and helps you to implement data ownership control. The `rbac/interceptor`
should be placed [inside](tutorials.md#interceptor-overriding) [db-access](interceptors.md#db-access).

### Role set definition

For [tiny-RBAC](https://github.com/Flexiana/tiny-rbac) you should provide
a [role-set](https://github.com/Flexiana/tiny-rbac#builder), a map, which defines the application resources, the actions
on it, the roles with the different granted actions, and restrictions for data ownership control. This map must be
in [deps](conventions.md#dependencies).

An example role-set can be defined for an image service:

```clojure
(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)))
```

It defines a role-set with:

- an `:image` resource,
- `:upload :download :delete` actions on `:image` resource
- a `:guest` role, who can download all the images
- a `:member` role, who inherits `:guest`'s roles, and it can upload `:all` images, and delete `:own` images.

### Provide resource/action at routing

The resource and action can be defined on route definition. The RBAC interceptor will check permissions against what is
defined here:

```clojure
(def routes
  [["/api" {:handler handler-fn}
    ["/image" {:get    {:action     get-image
                        :permission :image/download}
               :put    {:action     add-image
                        :permission :image/upload}
               :delete {:action     delete-image
                        :permission :image/delete}}]]])
```

### Application start-up

```clojure
(def role-set
  (-> (b/add-resource {} :image)
      (b/add-action :image [:upload :download :delete])
      (b/add-role :guest)
      (b/add-inheritance :member :guest)
      (b/add-permission :guest :image :download :all)
      (b/add-permission :member :image :upload :all)
      (b/add-permission :member :image :delete :own)))

(def routes
  [["/api" {:handler handler-fn}
    ["/login" {:action       login
               :interceptors {:except [session/interceptor]}}]
    ["/image" {:get    {:action     get-image
                        :permission :image/download}
               :put    {:action     add-image
                        :permission :image/upload}
               :delete {:action     delete-image
                        :permission :image/delete}}]]])

(defn system
  [config]
  (let [session-backend (:session-backend config (session-backend/init-in-memory))
        deps {:webserver               (:framework.app/web-server config)
              :routes                  (routes/reset routes)
              :session-backend         session-backend
              :role-set                role-set
              :controller-interceptors [interceptors/params
                                        session/interceptor
                                        rbac/interceptor
                                        interceptors/db-access]
              :db                      (db-core/start
                                         (:framework.app/postgresql config))}]
    (update deps :webserver (ws/start deps))))

(defn -main
  [& _args]
  (system (config/env)))

```

### Access control

Prerequisites:

- role-set in `(-> state :deps :role-set)`
- route definition has `:permission` key
- user's role is in `(-> state :session-data :users/role)`

If the `:permission` key is missing, all requests going to be **granted**. If `role-set` or `:users/role` is missing ,
all requests going to be **denied**.

When `rbac/interceptor` `:enter` executed it will check if the user has any permission on the
pre-defined `resource/action`
pair. If any, it will collect all of them (including inherited permissions) into a set of format:
`:resource/restriction`.

For example:

```clojure
:image/own
```

means the given user granted to do the given `action` on `:own` `:image` resource. This will help you to
implement [data ownership](#data-ownership) functions. This set is associated
in `(-> state :request-data :user-permissions)`

If the user cannot do the given action on the given resource (neither by inheritance) the interceptor will interrupt the
execution flow, and will create a response:

```clojure
{:status 403
 :body   "Forbidden"}
```

### Data ownership

Data ownership control is about tightening the database result for those elements where the user is able to do the given
action. For staying the example above, it means a `:member` only able to delete `:image`s if it owned by the `:member`.
At this point, you can use the result of the [access control](#access-control) from the state. Let's stay in the example:

From

```clojure
{:delete [:*]
 :from   [:images]
 :where  [:= :id (get-in state [:params :image-id])]}
```

database query you want to generate something like this:

```clojure
{:delete [:*]
 :from   [:images]
 :where  [:and
          [:= :id (get-in state [:params :image-id])]
          [:= :owner.id user-id]]}
```

To achieve this, you can simply provide a `restriction function` into `(-> state :request-data :restriction-fn)`
The `user-permissions` is a set, so it can be easily used for making conditions:

```clojure
(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/own) (let [user-id (get-in state [:session-data :users/id])]
                                      (xiana/ok (update state :query sql/merge-where [:= :owner.id user-id])))
      :else (xiana/ok state))))
```

And finally the only missing piece of code: the model, and the action

```clojure

(defn delete-query
  [state]
  {:delete [:*]
   :from   [:images]
   :where  [:= :id (get-in state [:params :image-id])]})

(defn delete-image
  [state]
  (xiana/ok
    (-> state
        (assoc :query (delete-query state))
        (assoc-in [:request-data :restriction-fn] restriction-fn))))
```
