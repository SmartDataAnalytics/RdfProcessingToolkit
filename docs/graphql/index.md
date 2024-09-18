---
title: GraphQL over SPARQL
has_children: true
nav_order: 100
layout: default
---

# GraphQL over SPARQL

This section describes out approach to generate JSON responses from SPARQL endpoints using GraphQL as a query and mapping language.
Mutations are not supported.
The GraphQL queries of our approach are self-contained, i.e. no additional server configuration is needed. The endpoint is avilable when running [`rpt integrate --server`](../integrate), by default [http://localhost:8642/graphql](http://localhost:8642/graphql).
From each GraphQL query a corresponding SPARQL query and result set post processor is created.

## Examples

Check out the [demonstrators](../demos) for examples.

## Directives

The following is the set of supported directives:

[`@prefix`](reference/prefix) [`@pattern`](reference/pattern) [`@one` and `@many`](reference/one-and-many) [`@index`](reference/index-directive) [`@join`](reference/join)
