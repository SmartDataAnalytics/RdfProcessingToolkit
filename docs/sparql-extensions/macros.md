---
title: User Defined Functions
parent: SPARQL Extensions
nav_order: 30
layout: default
---

# User Defined Functions (Macros)

RPT supports defining macros in RDF. 
An IRI with a `udf:simpleDefinition` property can be used as a function IRI in SPARQL.

The object needs to be an `rdf:List`, where:

* The first argument must be a string with a syntactically valid SPARQL expression
* Any further argument is interpreted as a variable name.

## Example

The following example defines a custom `eg:greet` function that returns `Hello X!` for an argument `X`.

```
# macros.ttl
PREFIX udf: <https://w3id.org/aksw/norse#udf.>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX eg: <http://www.example.org/>

eg:prefixMapping
  sh:declare [ sh:prefix "afn" ; sh:namespace "http://jena.apache.org/ARQ/function#" ]
  .

eg:greet udf:simpleDefinition ("CONCAT('Hello ', STR(?x), '!')" "x") .
```

## Using the Macros

Macros are specified using the `--macro` option and can be used throughout the system, i.e. the CLI arguments, and the SPARQL and GraphQL endpoints.

```bash
rpt integrate --macro macros.ttl `SELECT (eg:greet('John Doe') AS ?x) {}` --out-format txt
```

```
---------------------
| x                 |
=====================
| "Hello John Doe!" |
---------------------
```

## Notes

* A current limitation in our implementation is that the `sh:namespace` presently needs to be a string rather than a literal of type `xsd:anyUri`, as demanded by the [SHACL Specification](https://www.w3.org/TR/shacl/).
* Also note, that due to SHACL's design that the value of `sh:namespace` needs to be a literal, it is NOT possible to refer to a namespace declared on the document itself:

```
PREFIX eg: <http://www.example.org/>
PREFIX sh: <http://www.w3.org/ns/shacl#>

eg:prefixMapping
  sh:declare [ sh:prefix "eg" ; sh:namespace eg: ] # Disallowed
```

