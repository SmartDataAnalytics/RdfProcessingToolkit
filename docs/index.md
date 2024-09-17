---
title: Overview
layout: home
nav_order: 10
---

# RDF Processing Toolkit (RPT)

RDF/SPARQL Workflows on the Command Line made easy. The RDF Processing Toolkit (RPT) integrates several of our tools into a single CLI frontend:
It features commands for running SPARQL-statements on triple and quad based data both streaming and static.
SPARQL extensions for working with CSV, JSON and XML are included. So is an RML toolkit that allows one to convert RML to SPARQL (or TARQL).
RPT ships with Jena's ARQ and TDB SPARQL engines as well as one based on Apache Spark.

The [`integrate`](integrate) command is the most versatile one: It accepts as arguments rdf files and sparql query/update statements which are run in a pipeline. The  `--server` option starts a web server with SPARQL and GraphQL endpoints over the provided data.
Using `integrate` with the `remote` engine allows RPT to act as a [SPARQL proxy](integrate/#example-4-sparql-proxy).

RPT is Java tool which comes with debian and rpm packaging. It is invoked using `rpt <command>` where the following commands are supported:

* [integrate](integrate): This command is the most relevant one for day-to-day RDF processing. It features ad-hoc querying, transformation and updating of RDF datasets with support for SPARQL-extensions for ingesting CSV, XML and JSON. Also supports `jq`-compatible JSON output that allows for building bash pipes in a breeze.
* [ngs](named-graph-streams): Processor for named graph streams (ngs) which enables processing for collections of named graphs in streaming fashion. Process huge datasets without running into memory issues.
* [sbs](sparql-binding-streams): Processor for SPARQL binding streams (sbs) which enables processing of SPARQL result sets in streaming fashion. Most prominently for use in aggregating the output of a `ngs map` operation.
* [rmltk](https://github.com/Scaseco/r2rml-api-jena/tree/jena-5.0.0#usage-of-the-cli-tool): These are the (sub-)commands of our (R2)RML toolkit. The full documentation is available [here](https://github.com/SmartDataAnalytics/r2rml-api-jena).
* sansa: These are the (sub-)commands of our Semantic Analysis Stack (Stack) - a Big Data RDF Processing Framework. Features parallel execution of RML/SPARQL and TARQL (if the involved sources support it).


**Check this [documentation](doc) for the supported SPARQL extensions with many examples**

