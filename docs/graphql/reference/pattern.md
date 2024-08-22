---
title: pattern
parent: GraphQL
nav_order: 120
layout: default
---

## GraphQL Directive: `@pattern`

The `@pattern` directive is used to associate a field in your GraphQL schema with a SPARQL graph pattern. It allows you to define how variables within a SPARQL query map to the fields in your schema, providing flexibility in connecting fields to specific parts of the SPARQL graph.

### Purpose

The directive is particularly useful in RDF and knowledge graph scenarios, where the data is modeled as triples. It lets you specify how variables in the pattern relate to each other across different fields.

#### Key Concepts

- **Source Variables (`from`)**: These variables represent the starting point of the field’s graph pattern. They typically join with the parent field’s target variables by default.
- **Target Variables (`to`)**: These variables represent the output of the field’s graph pattern.
- **SPARQL Pattern (`of`)**: The SPARQL graph pattern, expressed as a string, which specifies the relationship between variables.

#### Usage

The `@pattern` directive supports the following arguments:

- **`of`** (`String`): The SPARQL graph pattern that defines the relationship between variables.
- **`from`** (`String | [String]`): The source variable(s) for this field. If only a single variable is used, it can be passed directly as a string. Otherwise, an array is used.
- **`to`** (`String | [String]`): The target variable(s) for this field. Similar to `from`, this can be a string or an array.

#### Example

Here is an example demonstrating how to define a `MusicalArtists` field using the `@pattern` directive:

```graphql
{
  MusicalArtists @pattern(of: "?s a dbo:MusicalArtist", from: "s", to: "s") {
    label @pattern(of: "?s rdfs:label ?o", from: "s", to: "o")
  }
}
```

#### Explanation

1. **MusicalArtists Field**: The `MusicalArtists` type is associated with the graph pattern `?s a dbo:MusicalArtist`, where the `s` variable acts as both the source and target.
2. **Label Field**: The `label` field is defined with a nested pattern `?s rdfs:label ?o`, where `s` is the source and `o` is the target.

#### Rule for Implicit Joins

By default, a field’s source variables (defined by `from`) are automatically joined with its parent’s target variables (defined by `to`). This allows seamless chaining of patterns without redundant variable specification. The precise join type is a [LATERAL join](https://github.com/w3c/sparql-dev/issues/100).

#### Notes

- If there is only one source or target variable, the array brackets (`[]`) can be omitted.
- This directive is designed to handle more complex SPARQL graph patterns and facilitate better integration with RDF data sources.


