# RDF Processing Toolkit

## News

* 2023-05-19 New quality of life features: `cpcat` command and the canned queries `tree.rq` and `gtree.rq`.
* 2023-04-04 Release v1.9.5! RPT now ships with `sansa` (Apache Spark based tooling) and `rmltk` (RML Toolkit) features. A proper GitHub release will follow once Apache Jena 4.8.0 is out as some code depends on its latest SNAPSHOT changes.
* 2023-03-28 Started updating documentation to latest changes (ongoing)

[Previous entries](#History)


## Overview

RDF/SPARQL Workflows on the Command Line made easy. The RDF Processing Toolkit (RPT) integrates several of our tools into a single CLI frontend:
It features commands for running SPARQL-statements on triple and quad based data both streaming and static.
SPARQL extensions for working with CSV, JSON and XML are included. So is an RML toolkit that allows one to convert RML to SPARQL (or TARQL).
Ships with Jena's ARQ and TDB SPARQL engines as well as one based on Apache Spark.

RPT is Java tool which comes with debian and rpm packaging. It is invoked using `rpt <command>` where the following commands are supported:

* [integrate](README-SI.md): This command is the most relevant one for day-to-day RDF processing. It features ad-hoc querying, transformation and updating of RDF datasets with support for SPARQL-extensions for ingesting CSV, XML and JSON. Also supports `jq`-compatible JSON output that allows for building bash pipes in a breeze.
* [ngs](README-NGS.md): Processor for named graph streams (ngs) which enables processing for collections of named graphs in streaming fashion. Process huge datasets without running into memory issues.
* [sbs](README-SBS.md): Processor for SPARQL binding streams (sbs) which enables processing of SPARQL result sets in streaming fashion. Most prominently for use in aggregating the output of a `ngs map` operation.
* [rmltk](https://github.com/Scaseco/r2rml-api-jena/tree/jena-5.0.0#usage-of-the-cli-tool): These are the (sub-)commands of our (R2)RML toolkit. The full documentation is available [here](https://github.com/SmartDataAnalytics/r2rml-api-jena).
* sansa: These are the (sub-)commands of our Semantic Analysis Stack (Stack) - a Big Data RDF Processing Framework. Features parallel execution of RML/SPARQL and TARQL (if the involved sources support it).


**Check this [documentation](doc) for the supported SPARQL extensions with many examples**

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


## Building
The build requires maven.

For convenience, this [Makefile](Makefile) which defines essential goals for common tasks.
To build a "jar-with-dependencies" use the `distjar` goal. The path to the created jar bundle is shown when the build finishes.
In order to build and and install a deb or rpm package use the `deb-rere` or `rpm-rere` goals, respectively.

```
$ make

make help                # Show these help instructions
make distjar             # Create only the standalone jar-with-dependencies of rpt
make rpm-rebuild         # Rebuild the rpm package (minimal build of only required modules)
make rpm-reinstall       # Reinstall rpm (requires prior build)
make rpm-rere            # Rebuild and reinstall rpm package
make deb-rebuild         # Rebuild the deb package (minimal build of only required modules)
make deb-reinstall       # Reinstall deb (requires prior build)
make deb-rere            # Rebuild and reinstall deb package
make docker              # Build Docker image
make release-bundle      # Create files for Github upload
```

A docker image is available at https://registry.hub.docker.com/r/aksw/rpt

The docker image can be built with a custom tag by setting the property `docker.tag`.
The default for `docker.tag` is `${docker.tag.prefix}${project.version}`, where `docker.tag.prefix` defaults to the empty string.
When only setting `docker.tag.prefix` to e.g. `myfork-` then the tag will have the form `myfork-1.2.3-SNAPSHOT`.

```bash
make docker

# Example for providing a custom docker tag via make:
make docker ARGS='-D"docker.tag.prefix=experimental-"'
```

## License
The source code of this repo is published under the [Apache License Version 2.0](LICENSE).
Dependencies may be licensed under different terms. When in doubt please refer to the licenses of the dependencies declared in the `pom.xml` files.
The dependency tree can be viewed with Maven using `mvn dependency:tree`.


## Acknowledgements

* This project is developed with funding from the [QROWD](http://qrowd-project.eu/) H2020 project. Visit the [QROWD GitHub Organization](https://github.com/Qrowd) for more Open Source tools!

## History

* (no entry yet)

