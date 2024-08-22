---
title: @one and @many
parent: GraphQL
nav_order: 30
layout: default
---

## GraphQL Directives: `@one` and `@many`

The `@one` and `@many` directives control the cardinality of a field within a GraphQL schema. These directives are particularly useful in RDF and SPARQL-based contexts, where fields often correspond to graph patterns that involve relationships with varying cardinalities.

### Purpose

These directives allow you to specify whether a field should be treated as a single-valued or multi-valued field. By default, fields are treated as multi-valued (`@many`), which is typical in RDF data where properties often have multiple values.

#### Arguments

Both directives accept the following arguments:

- **`self`** (`Boolean`): Controls whether the directive applies to the field it appears on.
- **`cascade`** (`Boolean`): Controls whether the directive cascades to child fields, affecting their cardinality as well.

#### Default Behavior

- **`@many`** is the default cardinality for all fields, as fields are often mapped to RDF graph patterns that can yield multiple target values (1:n relationships).
- When applied, the directives determine whether a field is considered single-valued (`@one`) or multi-valued (`@many`), and can optionally cascade this behavior to child fields.

#### Usage

These directives can be applied to fields to control their cardinality and the cardinality of their child fields:

- **`self`** (`true` by default): If `true`, the directive applies to the field itself.
- **`cascade`** (`false` by default): If `true`, the directive applies to all child fields as well.

#### Example

Consider the following example:

```graphql
type Parent @one(self: false, cascade: true) { 
  # The Parent field is still effectively @many, but the cardinality cascades to its children
  Child1 # Child1 inherits @one cardinality from Parent
  Child2 # Child2 also inherits @one cardinality from Parent
}
```

#### Detailed Explanation

1. **Parent Field**: The `@one(self: false, cascade: true)` directive is applied. This configuration means that:
   - `self: false`: The `@one` directive does **not** apply to the `Parent` field itself. The field remains multi-valued (`@many`).
   - `cascade: true`: The `@one` behavior cascades to the child fields (`Child1` and `Child2`), making them single-valued.

2. **Child Fields**: Both `Child1` and `Child2` automatically inherit the `@one` cardinality from the `Parent` due to the cascading effect. They are treated as single-valued fields.

#### Understanding Cardinality Control

- **`@one` Directive**: Specifies that the field is single-valued. If a field mapped to a SPARQL pattern yields more than one value, it will trigger an error in the GraphQL output.
- **`@many` Directive**: Specifies that the field is multi-valued, allowing it to contain an array of values (this is the default).

#### Practical Use Cases

The `@one` and `@many` directives are useful when you need precise control over the expected cardinality of fields, especially in cases where:
- You expect a single value (e.g., a unique identifier or singular property) and want to enforce this constraint.
- You want to apply consistent cardinality rules across a hierarchy of fields using cascading behavior.

#### Notes

- By default, fields are assumed to be `@many` unless explicitly overridden.
- The `cascade` argument allows you to propagate cardinality rules down to child fields, reducing the need for redundant annotations.

