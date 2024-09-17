---
title: Overview
layout: home
nav_order: 10
---

# RDF Processing Toolkit (RPT)

RPT is a SPARQL-centric command line toolkit for processing RDF data that also comes with an integrated a web server.
The `integrate` command is the most powerful one: It accepts as arguments rdf files and sparql query/update statements which are run in a pipeline. The  `--server` option starts a web server with SPARQL and GraphQL endpoints over the provided data.
RPT can also function as a SPARQL proxy using the `remote` engine.



## Command Overview

* [`integrate`](integrate): Powerful RDF and SPARQL processing with a [GraphQL](graphql).

* `rmltk`: R2RML and RML toolkit. Converts mappings to SPARQL and executes them

* `sansa`: Big Data (based on Apache Spark and Apache Hadoo)

    

