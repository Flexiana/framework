# Introduction to framework

TODO: write [great documentation](http://jacobian.org/writing/what-to-write/)



## Email configuration

The framework has support for sending email messages.
For this functionality to work, a `:framework.app/emails` key has to be added to the application 
config e.g. `config/dev/config.edn`. 
`:from` is a required key. `:host :user :pass :tls :port` are optional, if they are not present, 
the app will default to the mail sender present on the host, but you can use any SMTP of your 
choice. 

Full configuration looks like this

```clojure
:framework.app/emails             {:host "smtp.some.mail.com"
                                   :user  "john.user"
                                   :pass  "secret@password"
                                   :tls true
                                   :port 587
                                   :from "System Admin <sysadmin@example.com>"}
```
