---
title: bind
parent: GraphQL
nav_order: 130
layout: default
---

## GraphQL Directive: `@bind`

The `@bind` directive is used to associate a field in your GraphQL query or schema with a SPARQL expression. Note that [`@pattern`](pattern) is for graph patterns rather than expressions.
As bind only produces a single value from the input binding, its cardinality defaults to `@one`. Explicitly adding `@many` will cause the value to be wrapped in an array.

### Purpose

The directive allows one to compute values for a field based on variables that are mentioned in any ancestor node. The most common use is to expose an entity's IRI (or blank node) as an `id` field.

#### Usage

The `@bind` directive supports the following arguments:

- **`of`** (`String`): The value of `of` is a SPARQL expression. The mentioned variables must be defined in any ancestor or at the annotated field.
- **`as`** (`String`): (optional) The variable for the expression result can be specified explicitly (by default an internal name is generated).
- **`target`** (`Boolean`): (optional, defaults to `true`) Whether the `@bind` expression produces the target values for the annotated field.
    If `target` is `false` then the field introduces the specified variable that can be referenced from descendants without it becoming a target of the field.
    Multiple target variables are combined into a tuple based on the order of their appearance.

#### Example

Here is an example demonstrating how to define a `MusicalArtists` field using the `@bind` directive:

```graphql
{
  MusicalArtists @pattern(of: "?s a <http://dbpedia.org/ontology/MusicalArtist>") {
    id @bind(of: "?s")
  }
}
```

