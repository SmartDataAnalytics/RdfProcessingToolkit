---
title: @join
parent: GraphQL
nav_order: 50
layout: default
---

## GraphQL Directive: `@index`

The `@index` directive allows you to transform the output of a GraphQL field into a JSON object, where the keys are derived from a specified SPARQL expression. This is especially useful when you need to index data by certain variables in a SPARQL query and control the structure of the JSON output based on the cardinality of the data.

### Purpose

This directive is designed to facilitate the indexing of field outputs by a specified SPARQL expression, turning the field into a JSON object where the keys are determined by that expression. Additionally, it allows you to control the cardinality (i.e., whether the value for each key is a single item or an array) using a `oneIf` condition.

#### Arguments

- **`by`** (`String`): A SPARQL expression that determines the keys in the indexed output.
- **`oneIf`** (`String`): A SPARQL expression that controls whether the value for each key should be treated as a single item (if the expression evaluates to `true`) or an array (if it evaluates to `false`). By default, this is set to `"false"`, meaning the value is treated as an array unless explicitly overridden.

#### How It Works

The `@index` directive converts a field’s output into a JSON object, where:
- The keys are derived from the `by` argument.
- The values are determined based on the `oneIf` argument:
  - If `oneIf` evaluates to `true`, a single value is expected for each key.
  - If `oneIf` evaluates to `false`, multiple values are allowed, and they are returned as an array.

#### Example

Consider the following GraphQL schema:

```graphql
type Triples @pattern(
  of: "?s ?p ?o", 
  from: "s", 
  to: "o"
) @index(by: "?p", oneIf: "false")
```

Given the RDF triples:

```sparql
PREFIX : <http://www.example.org/>
:s1 :p1 :o1 .
:s1 :p2 :o2 .
:s2 :p1 :o1 .
```

The expected JSON output would be:

```json
{
  "http://www.example.org/p1": ["http://www.example.org/o1", "http://www.example.org/o1"],
  "http://www.example.org/p2": ["http://www.example.org/o2"]
}
```

#### Detailed Explanation

1. **Indexing by SPARQL Expression**: The `by` argument specifies that the output should be indexed by the value of `?p`. Each unique value of `?p` becomes a key in the resulting JSON object.

2. **Handling Cardinality with `oneIf`**: The `oneIf` argument is set to `"false"`, meaning that the value for each key is treated as an array, allowing multiple values for the same key. If the `oneIf` condition were set to a SPARQL expression that evaluates to `true`, only a single value would be allowed for each key, and any additional values would trigger an error.

3. **Default Behavior**: If the `oneIf` argument is omitted, it defaults to `"false"`, meaning the values are treated as arrays unless explicitly specified otherwise.

#### Notes

- The `by` argument must be a valid SPARQL expression, typically a variable or an expression that resolves to a value.
- The `oneIf` argument provides flexibility in defining whether the output for each key should be a single value or an array, depending on the cardinality of the data.
- When the `oneIf` condition is `true` for a key, only one value is expected. If multiple values are encountered, an error will be reported in the GraphQL output.

#### Practical Use Cases

The `@index` directive is particularly useful when:
- You need to convert a list of triples into a JSON object indexed by a specific predicate or property.
- You want to control how cardinality is handled in your JSON output, deciding whether to treat the values as single items or arrays based on SPARQL conditions.


