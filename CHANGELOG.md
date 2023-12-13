## Unreleased

## 0.5.0-rc6

- kebab camel interceptor for all request type #263
- dev/reset migratus #264
- Add swagger documentation #266
- Feature/swagger toplevel description #268
- Bump hiccup/hiccup to version 2.0.0-RC2 #269

## 0.5.0-rc5

- Bump info.sunng/ring-jetty9-adapter ring adapter library
- Refactor swagger

## 0.5.0-rc4

- added swagger-ui
- Fix `xiana.db/in-transaction` to support any `java.sql.Connection`
- Add support for custom datasource
- JWT signing support for authentication and content exchange via interceptors.

## 0.5.0-rc3

- Use clj-tools for
    - test
    - build
    - install
    - release
- Remove `project.clj`
- Fix kibit complaints
- Added new `prune-get-request-bodies` interceptor

Breaking changes:

- refactor database migration system, use patched migratus (Flexiana/migratus), removed seed module and rewrite data
  migration tool

## 0.5-rc2

Breaking changes:

- removed monands (`funcool.cats` library), related code and docs
  removed `xiana.interceptor.wrap/interceptor`
- changed interceptors behaviour making them similar to pedestal and sieppari
- changed error handling
- addded deps.edn
- a bunch of other small fixes.

## 0.5-rc1

Breaking change: rename almost all namespaces from `framework..` to `xiana..`).
