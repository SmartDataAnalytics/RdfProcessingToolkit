---
title: Embedded SPARQL Engines
parent: RDF/SPARQL Processing
nav_order: 20
has_children: false
layout: default
---

# Embedded SPARQL Engines

The following engines can be used using `rpt integrate -e engine [--loc engine-specific-location]`.

Embedded SPARQL engines are built into RPT and thus readily available. The following engines are currently available:

<table>
    <tr><th>Engine</th><th>Description</th></tr>
    <tr><td><b>mem</b></td><td>The default in-memory engine based on Apache Jena. Data is discarded once the RPT process terminates.</td></tr>
    <tr><td><b>tdb2</b></td><td>Apache Jena's TDB2 persisent engine. Use <i>--loc</i> to specfify the database folder.</td></tr>
    <tr><td><b>binsearch</b></td><td>Binary search engine that operates directly on sorted N-Triples files. Use <i>--loc</i> to specify the file path or HTTP(s) URL to the N-Triples file. For URLs, HTTP range requests must be supported!</td></tr>
    <tr><td><b>remote</b></td><td>A pseudo engine that forwards all processing to the SPARQL endpoint whole URL is specified in <i>--loc</i>.</td></tr>
    <tr><td><b>qlever</b></td><td>The blazing fast <a href="https://github.com/ad-freiburg/qlever">qlever</a> triple store launched from its docker image via Java's TestContainers framework. Use <code>qlever:imageName:tag</code> to use a specific image - this image's command line interfaces for starting the server and creating the indexes must be compatible with the default image registered with RPT.</td></tr>
</table>

### (ARQ) Engine Configuration

The engines `mem`, `tdb2` and `binsearch` build an Jena's query engine `ARQ` and thus respect its configuration.

`rpt integrate  --set 'arq:queryTimeout=60000' --set 'arq:updateTimeout=1800000' data.ttl myUpdate.ru myQuery.rq`




