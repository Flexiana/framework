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

Xiana framework don't have login and logout functions because all application can be different on user management. The
session interceptor can validate a request only if the session is already exists in session storage. So to login a user,
you need to add its session to the storage. All session should have unique, and valid UUID as session-id, and this ID
should be part of active session too. The active session goes to the `(-> state :session-data)`. It's loaded and stored
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
  (xiana/ok
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
resources. It's a role based access control and helps you to implement data ownership control. The `rbac/interceptor`
should be placed [inside](tutorials.md#interceptor-overriding) [db-access](interceptors.md#db-access).

### Role set definition

For [tiny-RBAC](https://github.com/Flexiana/tiny-rbac) you should provide
a [role-set](https://github.com/Flexiana/tiny-rbac#builder), a map, which defines the application resources, the actions
on it, and the roles with the different granted actions, and restrictions for data ownership control. This map must be
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
- user's role is in `(-> state :session-data :user :users/role)`

If the `:permission` key is missing, all requests going to be **granted**. If `role-set` or `:users/role` is missing ,
all requests going to be **denied**.

When `rbac/interceptor` `:enter` executed it'll check if the user has any permission on the
pre-defined `resource/action`
pair. If it has any, it will collect all of them (including inherited permissions) into a set in a format:
`:resource/restriction`.

For example:

```clojure
:image/own
```

means the given user granted to do the given `action` on `:own` `:image` resource. This will help you to
implement [data ownership](#data-ownership) functions. This set is associated
in `(-> state :request-data :user-permissions)`

If the user cannot do the given action on the given resource (neither by inheritance) the interceptor will interrupt the
execution flow, and creates a response:

```clojure
{:status 403
 :body   "Forbidden"}
```

### Data ownership

Data ownership control is about tightening the database result for those elements where the user is able to do the given
action. For staying the example above, it means a `:member` only able to delete `:image`s if it owned by the `:member`.
At this point you can use the result of the [access control](#access-control) from state. Let's stay in the example:

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
      (user-permissions :image/own) (let [user-id (get-in state [:session-data :user :users/id])]
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

### All together

```clojure
(ns app.core
  (:require
    [app.action.image :refer [get-image
                              add-image
                              delete-image]]
    [app.action.login :refer [login]]
    [tiny-rbac.builder :as b]
    [framework.handler.core :refer [handler-fn]
     [framework.session.core :as session]
     [framework.route.core :as routes]
     [framework.interceptor.core :as interceptors]
     [framework.db.core :as db-core]
     [framework.webserver.core :as ws]
     [framework.config.core :as config]
     [framework.rbac.core :as rbac]]))

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
  (let [session-backend (:session-backend config (session/init-in-memory))
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

```clojure
(ns app.action.image
  (:require
    [app.model.image :refer [delete-query
                             restriction-fn]]
    [xiana.core :as xiana]))

(defn delete-image
  [state]
  (xiana/ok
    (-> state
        (assoc :query (delete-query state))
        (assoc-in [:request-data :restriction-fn] restriction-fn))))
```

```clojure
(ns app.model.image
  (:require
    [honeysql.helpers :as sql]
    [xiana.core :as xiana]))

(defn delete-query
  [state]
  {:delete [:*]
   :from   [:images]
   :where  [:= :id (get-in state [:params :image-id])]})

(defn restriction-fn
  [state]
  (let [user-permissions (get-in state [:request-data :user-permissions])]
    (cond
      (user-permissions :image/own) (let [user-id (get-in state [:session-data :user :users/id])]
                                      (xiana/ok (update state :query sql/merge-where [:= :owner.id user-id])))
      :else (xiana/ok state))))
```
