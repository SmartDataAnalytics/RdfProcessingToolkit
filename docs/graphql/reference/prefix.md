---
title: prefix
parent: GraphQL Reference
nav_order: 10
layout: default
---

## GraphQL Directive: `@prefix`

The `@prefix` directive is designed to manage and define namespace prefixes in a GraphQL schema. This directive can be used to either specify a single prefix with an IRI or map multiple prefixes to their corresponding IRIs.

### Usage

The `@prefix` directive accepts two possible configurations:

1. **Single Prefix Definition**: Use the `name` and `iri` arguments to define a single prefix.
2. **Multiple Prefix Mapping**: Use the `map` argument to define multiple prefixes in a key-value format.

#### Arguments

- **`name`** (`String`): The prefix name to be used.
- **`iri`** (`String`): The IRI (Internationalized Resource Identifier) associated with the prefix.
- **`map`** (`Map<String, String>`): A map of prefix names to their corresponding IRIs.

#### Examples

1. **Single Prefix Definition**

   Define a single prefix using the `name` and `iri` arguments:

   ```graphql
   @prefix(name: "rdf", iri: "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
   ```

2. **Multiple Prefix Mapping**

   Define multiple prefixes using the `map` argument:

   ```graphql
   @prefix(
     map: {
       rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
       rdfs: "http://www.w3.org/2000/01/rdf-schema#"
     }
   )
   ```

#### Notes

- When using the `map` argument, you can define multiple prefix-to-IRI mappings in a single directive instance.
- The `name` and `iri` arguments should not be used together with the `map` argument; choose one configuration based on your needs.

