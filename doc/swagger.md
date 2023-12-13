# Swagger documentation

## config

You need to specify your swagger.json and swagger-ui endpoints at the config file

    :xiana/swagger         {:uri-path "/swagger/swagger.json"
                            :path :swagger.json
                            :data {:coercion (reitit.coercion.malli/create
                                              {:error-keys #{:coercion :in :schema :value :errors :humanized}
                                               :compile malli.util/closed-schema
                                               :strip-extra-keys true
                                               :default-values true
                                               :options nil})
                                   :middleware [reitit.swagger/swagger-feature]
                                   ;;; This is toplevel info for your project swagger-ui page
                                   :info {:title "Your Project Title goes HERE"
                                          :description "Some description"
                                          :version "0.1"}}}
    :xiana/swagger-ui      {:uri-path "/swagger/swagger-ui"}

## System config

To add the `/swagger/swagger.json` and `/swagger/swagger-ui` endpoints you need
to add **xiana.swagger/add-swagger-endpoints** to your system configuration before
the **routes/reset**

    (defn ->system
      [app-cfg]
      (-> (config/config app-cfg)
          ...
          xsw/add-swagger-endpoints
          routes/reset
          ...
          ws/start))

## Routes creations

### Description

If you want a description for your endpoint on the **swagger-ui** page, include it in your route in the map
associated with the swagger key as below:

    ["/users" {:get {:description "This is the description that will appear on swagger-ui under this endpoint"
                     :action #'users/get-all-users}}]

### Parameters

At the moment **xiana.swagger** will not generate all the data for the endpoints
automaticaly. You need to specify whether the endpoint needs :body :path or :query
parameters and you need to specify those parameters. The good news is that if
you already have a malli schema for your endpoint, you can reuse that.

At the moment **xiana.swagger** will ONLY work with malli schemas!

    ["/users" {:post {:action #'users/add-users
                      :schema UsersAddReqPayload
                      :parameters {:body UsersAddReqPayload}
                      :description "This is the description that will appear on swagger-ui under this endpoint"}}]

If you don't already use a schema you can specify it inline like this:

    ["/users" {:post {:action #'users/add-users
                      :parameters {:body [:map {:closed true}
                                          [:name string?]
                                          [:age int?]]}
                      :description "This is the description that will appear on swagger-ui under this endpoint"}}]

