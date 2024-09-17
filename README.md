# RDF Processing Toolkit

## News

* 2023-05-19 New quality of life features: `cpcat` command and the canned queries `tree.rq` and `gtree.rq`.
* 2023-04-04 Release v1.9.5! RPT now ships with `sansa` (Apache Spark based tooling) and `rmltk` (RML Toolkit) features. A proper GitHub release will follow once Apache Jena 4.8.0 is out as some code depends on its latest SNAPSHOT changes.
* 2023-03-28 Started updating documentation to latest changes (ongoing)

[Previous entries](#History)


## Example Usage

* `integrate` allows one to load multiple RDF files and run multiple queries on them in a single invocation. Further prefixes from a snapshot of [prefix.cc](https://prefix.cc) are predefined and we made the SELECT keyword of SPARQL optional in order to make scripting less verbose. The `--jq` flag enables JSON output for interoperability with the conventional `jq` tool


```
rpt integrate data.nt update.ru more-data.ttl query.rq

rpt integrate --jq file.ttl '?s { ?s a foaf:Person }' | jq '.[].s'
```

* `ngs` is your well known bash tooling such as `head`, `tail`, `wc` adapted to named graphs instead of lines of text
```
# Group RDF into graph based on consecutive subjects and for each named graph count the number of triples
cat file.ttl | ngs subjects | ngs map --sparql 'CONSTRUCT { ?s eg:triples ?c} { SELECT ?s COUNT(*) { ?s ?p ?o } GROUP ?s }

# Count number of named graphs
rpt ngs wc file.trig

# Output the first 3 graphs produced by another command
./produce-graphs.sh | ngs head -n 3
```

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

* `spo.rq`: Output triples from the default graph
* `gspo.rq`: Output quads from the named graphs
* `tree.rq`: Deterministically replaces all intermediate nodes with blank nodes. Intermediate nodes are those that appear both as subject and as objects. Useful in conjunction with `--out-format turtle/pretty` for formatting e.g. RML.
* `gtree.rq`: Named graph version of `tree.rq`
* `rename.rq`: Replaces all occurrences of an IRI in subject and object positions with a different one. Usage (using environment variables): `FROM='urn:from' TO='urn:to' rpt integrate data.nt rename.rq`
* `count.rq`: Return the sum of the counts of triples in the default graph and quads in the named graphs.
* `s.rq`: List the distinct subjects in the default graph

## Example Use Cases

* [Lodservatory](https://github.com/SmartDataAnalytics/lodservatory) implements SPARQL endpoint monitoring uses these tools in this [script](https://github.com/SmartDataAnalytics/lodservatory/blob/master/update-status.sh) called from this [git action](https://github.com/SmartDataAnalytics/lodservatory/blob/master/.github/workflows/main.yml).
* [Linked Sparql Queries](https://github.com/AKSW/LSQ) provides tools to RDFize SPARQL query logs and run benchmark on the resulting RDF. The triples related to a query represent an instance of a sophisticated domain model and are grouped in a named graph. Depending on the input size one can end up with millions of named graphs describing queries amounting to billions of triples. With ngs one can easily extract complete samples of the queries' models without a related triple being left behind.


## License
The source code of this repo is published under the [Apache License Version 2.0](LICENSE).
Dependencies may be licensed under different terms. When in doubt please refer to the licenses of the dependencies declared in the `pom.xml` files.
The dependency tree can be viewed with Maven using `mvn dependency:tree`.


## Acknowledgements

* This project is developed with funding from the [QROWD](http://qrowd-project.eu/) H2020 project. Visit the [QROWD GitHub Organization](https://github.com/Qrowd) for more Open Source tools!

## History

* (no entry yet)

