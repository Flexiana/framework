# Introduction to Xiana

TODO: write [great documentation](http://jacobian.org/writing/what-to-write/)



## Email configuration

Xiana has support for sending email messages.
For this functionality to work, a `:xiana/emails` key has to be added to the application 
config e.g. `config/dev/config.edn`. 
`:from` is a required key. `:host :user :pass :tls :port` are optional, if they are not present, 
the app will default to the mail sender present on the host, but you can use any SMTP of your 
choice. 

Full configuration looks like this

```clojure
:xiana/emails {:host "smtp.some.mail.com"
               :user "john.user"
               :pass "secret@password"
               :tls  true
               :port 587
               :from "System Admin <sysadmin@example.com>"}
```

## Hashing

The module `xiana.hash` provides secure Bcrypt, Scrypt and Pbkdf2 hashing for storing user passwords.
If you'd like to define some of them, their keys are: `:bcrypt :scrypt :pbkdf2`. If you don't define one of them, Bcrypt will be defined by default. You can setup the key within `config` derectory as well as its corresponding optional configuration. The values by default for each algorithm are defined as follows:

```clojure
:xiana/auth {:hash-algorithm  :bcrypt ;; Available values: :bcrypt, :scrypt, and :pbkdf2
             :bcrypt-settings {:work-factor 11}
             :scrypt-settings {:cpu-cost        32768 ;; Must be a power of 2
                               :memory-cost     8
                               :parallelization 1}
             :pbkdf2-settings {:type       :sha1 ;; Available values: :sha1 and :sha256
                               :iterations 100000}}
```

The module has `make` (it generate a hashed password) and `check` (it verifies the encrypted password against the current string) functions to deal with hashing process.

