# 2. Database basic architecture

Date: 2021-01-14

## Status

Proposed

## Context

1. The framework rationale requires a separation of concern between "code
is code, data is data" and because of that some libraries were not
able to proceed to be used in the application.

2. The framework also wants to be a complete solution which means that
our users should not be required to learn too many libraries in order
to be productive. Instead, the framework should provide layers of
indirection to wrap such functionalities and provide centralized
control over the features.

## Decision

- Usage of `yogthos/config` to handle config files
- Usage of `stuartsierra/components` to handle dependency management
- Usage of `honeysql` to handle SQL interactions
- Build a framework layer to handle migrations in `honeysql` style
- Build framework layer to wrap honeysql, honeysql-plugins, next-jdbc,
  hikari-cp and possible others lirbaries.

## Consequences

- [Positive] Framework is more aligned with its goals
- [Positive] Framework already have a `SQL` layer to be used
- [Negative] Choosing to wrap underlying libraries adds the work to
  keep our library in sync with new features they release
- [Negative] We were not able to use `integrant` as desired in the
  beginning because it violates the first rationale.
- [Negative] We were not able to use `duct` as desired because it
  violates the first rationale
