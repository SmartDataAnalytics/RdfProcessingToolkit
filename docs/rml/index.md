---
title: R2RML and RML
has_children: false
nav_order: 150
layout: default
---

# Knowledge Graph Construction with (R2)RML

RPT can translate RML and R2RML to SPARQL that can be run either with the [`integrate`](../integrate) (single-threaded) or the [`sansa query`](../sansa/query) (multi-threaded) commands.
Note, that the generated SPARQL queries can be also be executed against the SPARQL endpoint launched by `rpt integrate --server`. This can be used to test and inspect individual SPARQL queries and the corresponding (R2)RML mappings.

## Basic Usage

### Conversion of RML files to SPARQL

* Convert an RML file to a sequence of SPARQL queries using rmltk's `rml to sparql` command:

    ```bash
    rpt rmltk rml to sparql mapping.rml.ttl > mapping.raw.rml.rq
    ```

* Group and/or reorder SPARQL queries using rmltk's `optimize workload` command:

    ```bash
    rpt rmltk optimize workload mapping.raw.rml.rq --no-order > mapping.rml.rq
    ```

### Executing the mapping process

* ... using the single threaded Jena engine:

    ```bash
    rpt integrate mapping.rq
    ```

* Using RPT's parallel Spark-based executor:

    ```bash
    rpt sansa query mapping.rq
    ```


## Core Technology

The core technology, i.e. the API and SPARQL converters, are part of our [(R2)RML-Toolkit](https://github.com/Scaseco/R2-RML-Toolkit).



