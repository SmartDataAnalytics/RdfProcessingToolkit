---
title: prefix
parent: GraphQL over SPARQL
nav_order: 100
layout: default
---

## GraphQL Directive: `@prefix`

The `@prefix` directive is designed to manage and define namespace prefixes in a GraphQL query or schema. This directive can be used to either specify a single prefix with an IRI or map multiple prefixes to their corresponding IRIs.

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
    {
      field
        @prefix(name: "rdf", iri: "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        @prefix(name: "owl", iri: "http://www.w3.org/2002/07/owl#")
    }
    ```

2. **Multiple Prefix Mapping**

    Define multiple prefixes using the `map` argument. As GraphQL does not allow the empty string `""` as a key, the `name/iri` form can be used in conjunction with map:

    ```graphql
    query MyQuery
      @prefix(map: {
        rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        rdfs: "http://www.w3.org/2000/01/rdf-schema#"
      }, name: "", iri: "http://my.domain/ontology/)
    {
      # ...
    }
    ```

#### Notes

- When using the `map` argument, you can define multiple prefix-to-IRI mappings in a single directive instance.

