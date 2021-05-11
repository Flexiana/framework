<p align="center"><a href="https://github.com/Flexiana/Frankie" target="_blank"><span style="font-size: 250%;">Frankie</span></a></p>
<p align="center">
<a href="https://github.com/Flexiana/framework/actions/workflows/test.yml"><img src="https://github.com/Flexiana/framework/actions/workflows/test.yml/badge.svg"></a>
<a href="https://github.com/Flexiana/Frankie"><img src="https://img.shields.io/badge/downloads-Coming%20Soon-yellow" alt="Total Downloads"></a>
<a href="https://github.com/Flexiana/Frankie"><img src="https://img.shields.io/badge/license-FIXME-green" alt="License"></a>
</p>

<p align="center">
Xiana is the MVC framework for proper Web Application development.
</p>

# Index

TODO: Seçkin, After everything is done. Fill this header. May be, with a script?

# About

Frankie is the best parts of several services. Services like;
- Odoo,
- Notion,
- OneSign?.

# Xiana

A Framework for welcomes newcomers to the Clojure/script world.

## What

Xiana is an MVC framework that gives you idiomatic Clojure/script development tool for small-middle size web applications.

## Who

For newcomers to Clojure/script from various backgrounds; Ruby on Rails, Django, Laravel.

## When

For small-to-mid size, monolitic Web Applications.

# Rationale

- Idiomatic Clojure & ClojureScript where possible.
- Easy to develop monolithic applications (both client & server rendered)
- Easy to use & integrated experience of various parts including:
  - Database
    - SQL-first approach
    - Database migrations
  - Forms
    - including database-based
  - Validation
    - including Form-based
  - Auth
    - Authentication - sign in, sign out, password reset, sessions
    - Authorization - ACL, user roles, administration
  - I18n & L10n
    - Integrated into forms, validations, templates
    - Maybe use some existing locale files to support formattting, etc.
  - Caching
    - Caching library
    - Also middleware
  - Logging
  - Error handling
  - API
  - Security
    - Escaping
    - Recognition
    - Integrated to forms, validations
    - Extensible
    - Pure-functional where possible (probably using monadic approach)\
      - Every Router & Controller behavior is Monad which means:
        - It is Applicative Functor
          - Which means, you can "sequence" steps and use result in other steps
            - we will use something like threading macro `->` that will work in similar way as `do` in Haskell works.
              - `controller->` runs controller behaviors that get `[request response data]` and returns `[request response data]` - with potential error handling
              - `router-> ` runs routers with http-request and expects Action+Params or ` Nothing`
                - First router, that returns Action+Params, will route and the rest is not evaluated.

# Background

Not confused yet? Explain things we already know!

## Idiomatic Clojure/script

Are there any source on this?

## Purely Functional on Clojure/script

### Monads

cats?

# Knowledge Pool

## Functional Programming

## Clojure Style Guide

# Team

## Management Team

We all need Flexiana page for individuals?

- [Jiri Knesl](https://github.com/jiriknesl)
- [Chris Tagle](https://calendly.com/chris-at-flexiana)

## Core Team

TODO: Seçkin, fill the team and middle team as information sources that who are not in the core-team, now.

- [Giga Chokheli](giga.chokheli@flexiana.com)
- [Juan Ignacio Lopez](juan.ignacio.lopez@flexiana.com)
- [Nicolas Boskovic](nicolas.boskovic@flexiana.com)
- [Dimos Michailidis](dimos.michailidis@flexiana.com)
- [Ihor Makarenko](ihor.makarenko@flexiana.com)
- [Isaac Rocha](isaac.rocha@flexiana.com)
- [Piotr Kurnik](piotr.kurnik@flexiana.com)
- []()

## Middle Team

- [Panagiotis Koromilias](panagiotis.koromilias@flexiana.com)
- [Ian Fernandez](ian.fernandez@flexiana.com)
- [Jan Mikulas](jan.mikulas@flexiana.com)
- [Krisztian Gulyas](krisztian.gulyas@flexiana.com)
- []()

# Contributing

## FAQ?

I simply suggest that we need to create a simple leiningen script or easy way to "ask to developers". So, we can collect questions and categorize them and count them categorically.
So we can put them there.

## Git Flow?

## Bug Report

TODO: Seçkin, create a bug reporting template!

## Coding Style Guide

## Functional Programming

## Code-of-Conduct?

## Vulnerability Report

TODO: Seçkin, list the team members that responsible to track and hunt the vulnerabilities...

# Constituent

TODO: Seçkin, the components come here...

# Pre-Requirements

TODO: Seçkin, list the required software(s) or/and components...

Purpose: There are three major platform. We can select each one's #1 package manager and we can create a list of packages to install them selectively.

# Development Instructions

## Development usage

# Tutorials
    
# License

TODO: Seçkin, redirect the license badge to here!

# Referances

- [Django](https://github.com/django/django)
- [Rails](https://github.com/rails/rails)
- [Laravel](https://github.com/laravel/laravel)
- [Angular](https://github.com/angular/angular)
- [Express.js](https://github.com/expressjs/express)
- [Flask](https://github.com/pallets/flask)
- [Symfony](https://github.com/symfony/symfony)
- [CodeIgniter](https://github.com/bcit-ci/CodeIgniter)
- [CakePHP](https://github.com/cakephp/cakephp)
- [ASP.NET Core](https://github.com/dotnet/aspnetcore)
