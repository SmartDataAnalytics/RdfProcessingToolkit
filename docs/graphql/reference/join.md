---
title: join
parent: GraphQL over SPARQL
nav_order: 150
layout: default
---

## GraphQL Directive: `@join`

The `@join` directive allows you to explicitly define how variables in a parent field are joined with variables in a child field. This directive is particularly useful when the implicit join rule does not apply, such as when working with composite keys or specific variable subsets.

### Purpose

While implicit joins automatically connect a parent’s target variables with a child’s source variables, the `@join` directive provides fine-grained control for cases where:
- The parent field’s target variables form a composite key (e.g., multiple variables like `?cityName` and `?countryName`).
- You need to join only a subset of these variables with the child field’s source variables.

#### Arguments

- **`parent`** (`String | [String]`): Specifies the parent field's variable(s) to be joined.
- **`this`** (`String | [String]`): Specifies `this` field's variable(s) that should be connected with its parent’s variable(s).

#### Usage

The `@join` directive can be used when there is a need to manually specify how variables in a child field relate to those in a parent field, typically in more complex SPARQL scenarios.

#### Example

The following example demonstrates how to use the `@join` directive in a GraphQL schema:

```graphql
{
  Location @pattern(of: "?x :city ?cityName ; :country ?countryName",
                    from: ["cityName", "countryName"], to: ["cityName", "countryName"]) {

    cityName @pattern(of: "BIND(?x AS ?y)", from: "x", to: "y") @join(parent: "cityName")
}
```

#### Explanation

1. **Location Field**: The `Location` field’s pattern includes a composite key formed by `cityName` and `countryName` (with both acting as target variables).
2. **CityName Field**: The `cityName` field’s pattern maps the value of `x` to `y`, where `y` is effectively a bound copy of `x`.
3. **Explicit Join**: The `@join(parent: "cityName")` directive ensures that the `x` variable (specified in `from: "x"`) joins with the parent’s `cityName` variable instead of relying on the implicit join rule.

#### Variable Handling and Flexibility

- When there is only a single variable to be joined, the array brackets can be omitted (e.g., `"cityName"` instead of `["cityName"]`).
- The `@join` directive provides more control over complex joining scenarios, such as those involving composite keys or selective joins.

#### Implicit vs. Explicit Joins

- **Implicit Joins**: Automatically connect a parent’s target variables with a child’s source variables based on the default variable inheritance rule. No `@join` directive is needed in these cases.
- **Explicit Joins**: The `@join` directive is required when more specific joins are needed, such as connecting only a subset of composite keys or customizing how variables are linked.

#### Notes

- Use the `@join` directive when you need more precision in how variables between parent and child fields are connected, especially when dealing with complex data models or SPARQL patterns.


