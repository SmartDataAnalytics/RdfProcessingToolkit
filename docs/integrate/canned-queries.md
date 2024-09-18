---
title: Canned Queries
parent: RDF/SPARQL Processing
nav_order: 30
layout: default
---

## Canned Queries
RPT ships with several useful queries on its classpath. Classpath resources can be printed out using `cpcat`. The following snippet shows examples of invocations and their output:

### Overview
```bash
$ rpt cpcat spo.rq
CONSTRUCT WHERE { ?s ?p ?o }

$ rpt cpcat gspo.rq
CONSTRUCT WHERE { GRAPH ?g { ?s ?p ?o } }
```

Any resource (query or data) on the classpath can be used as an argument to the `integrate` command:

```
rpt integrate yourdata.nt spo.rq
# When spo.rq is executed then the data is queried and printed out on STDOUT
```

### Reference

The exact definitions can be viewed with `rpt cpcat resource.rq`.

* `spo.rq`: Output triples from the default graph.
* `gspo.rq`: Output quads from the named graphs.
* `spogspo.rq`: Output all triples followed by all quads.
* `tree.rq`: Deterministically replaces all intermediate nodes with blank nodes. Intermediate nodes are those that appear both as subject and as objects. Useful in conjunction with `--out-format turtle/pretty` for formatting e.g. RML.
* `gtree.rq`: Named graph version of `tree.rq`.
* `rename.rq`: Replaces all occurrences of an IRI in subject and object positions with a different one. Usage (using environment variables): `FROM='urn:from' TO='urn:to' rpt integrate data.nt rename.rq`
* `count.rq`: Return the sum of the counts of triples in the default graph and quads in the named graphs.
* `s.rq`: List the distinct subjects in the default graph.

