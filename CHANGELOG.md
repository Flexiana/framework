## 0.5-rc3
- JWT signing support for authentication and content exchange via interceptors.

Breaking changes: 
- refactor database migration system, use patched migratus (Flexiana/migratus), removed seed module and rewrite data migration tool

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
