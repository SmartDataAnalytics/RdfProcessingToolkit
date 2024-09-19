---
title: R2RML and RML
has_children: false
nav_order: 150
layout: default
---

# Knowledge Graph Construction with (R2)RML

RPT can translate RML and R2RML to SPARQL that can be run either with the [`integrate`](integrate) (single-threaded) or the [`sansa query`](sansa) (multi-threaded) commands.

## Basic Usage

### Conversion of RML files to SPARQL

```bash
# Convert an RML file to a sequence of SPARQL queries:
rpt rmltk rml to sparql mapping.rml.ttl > mapping.raw.rml.rq

# Group and/or reorder SPARQL queries:
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



