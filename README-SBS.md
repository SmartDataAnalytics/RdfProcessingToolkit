## SPARQL Binding Streams (SBS)

The `sbs`  tool allows one to pass a SPARQL result sets to a given query. The incoming result set replaces the given query's
query pattern - hence it is ignored and can be left empty such as in `sbs map -s 'SELECT * {}' input.rsj`.
The main use case is to post process result sets that are assembled from multiple individual queries such as the output of the [Named Graph Streams (NGS)](README-NGS.md) tool which is part of the bundle.

The typical conventions of the [RDF Processing Toolkit](README.md) apply:

* The `SELECT` keyword is optional - so `?s { ?s a foaf:Person}` is equivalent to `SELECT ?s WHERE { ?s a foaf:Person }`
* IRIs starting with `<env:>` will be substituted with environment variable values. There by `<env:USER>` yields a string and using double slashes such as `<env://JAVA_HOME> a IRI>` yields IRIs.

Example:
```bash
sparql-integrate -o txt '(<env:USER> AS ?user) {}'

-----------
| user    |
===========
| "raven" |
-----------
```

```bash
➜  sbs -h
Usage: sbs [-h] [COMMAND]
SPARQL Binding Streams Subcommands
  -h, --help
Commands:
  map     Map bindings via queries
  filter  Filter bindings by an expression
```

```bash
Usage: sbs map [-h] [-o=<outFormat>] [-s=<queries>]... [<nonOptionArgs>...]
Map bindings via queries
      [<nonOptionArgs>...]   Input files
  -h, --help
  -o, --out-format=<outFormat>

  -s, --sparql=<queries>     SPARQL statements; only queries allowed
```

### Supported Input Format
SBS builds upon Apache Jena's ResultSetMgr and thus supports all its formats.




### Building
SBS is part of the rdf-processing-toolkit build. Installing the debian package also makes the command `sbs` tool available.


### Example Usage

* Save a result set to a file (by default in json) and later see for each type how many subjects there are:
* * `sparql-integrate data.ttl '* { ?s a ?t } > types.srj`
  * `sbs -o txt -s '?t (COUNT(DISTINCT ?s)) {} GROUP BY ?t ORDER BY DESC (COUNT(DISTINCT ?s))' types.srj`
  * `cat types.srj | sbs -o txt -s '?t (COUNT(DISTINCT ?s)) {} GROUP BY ?t ORDER BY DESC (COUNT(DISTINCT ?s))` works as well




