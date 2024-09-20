---
title: RDF/SPARQL Processing
has_children: true
nav_order: 30
layout: default
---

# Command: `integrate`

`rpt integrate` is the command for mixed RDF data and SPARQL statement processing. The name stems from the various SPARQL extensions that make it possible to reference and process non-RDF data inside a SPARQL statements.

## Basic Usage

### Example 1: Simple Processing

`rpt integrate file1.ttl 'INSERT DATA { eg:s eg:p eg:o }' spo.rq`  

The command above does the following:

* It loads `file1.ttl` (into the default graph)
* It runs the given SPARQL update statement which adds a triple. For convenience, RPT includes a static copy of prefixes from [prefix.cc](https://prefix.cc). The prefix `eg` is defined as `http://www.example.org/`.
* It executes the query in the "file" `spo.rq` which is `CONSTRUCT WHERE { ?s ?p ?o }` and prints out the result. To be precise `spo.rq` is provided file in the JAR bundle (a class path resource). RPT ships with several predefined queries for common use cases.



#### Notes

* If you want RPT to print out the result of a query then you need to provide a query! If you omit `spo.rq` in the example above, rpt will only run the loading and the update.
* As alternatives for `spo.rq`, you can use `gspo.rq` to print out all quads and `spogspo.rq` to print out the union of triples and quads.
* The file extension `.rq` stands for `RDF query`. Likewise `.ru` stands for `RDF update`.



### Example 2: Starting a server

`rpt integrate --server`

This command starts a SPARQL server, by default on `port 8642`. Use e.g. `--port 8003` to pick a different one. You can mix this with the arguments from the first example.



* SPARQL endpoint and Yasgui frontend: http://localhost:8642/sparql
* GraphQL endpoint: http://localhost:8642/graphql
* Snorql frontend: http://localhost:8642/snorql
* Resource Viewer: <a href="http://localhost:8642/view/?*?http://www.wikidata.org/entity/Q1000094">http://localhost:8642/view/?*?http://www.wikidata.org/entity/Q1000094</a>

### Example 3: Loading Triples into Named Graphs

The option `graph=` (no whitespace before the `=`) sets the graph for all subsequent files. To use the default graph again, use `graph=` followed by a whitespace or simply `graph`.

```bash
rpt integrate graph=urn:foo file1.nt file2.ttl 'graph=http://www.example.org/' file3.nt.bz2 graph file4.ttl.gz
```


### Example 4: Using a different RDF Database Engine

RPT can run the RDF loading and SPARQL query execution on different (embedded) engines.

`rpt integrate --db-engine tdb2 --db-loc --db-keep mystores/mydata file.ttl spo.rq`

`rpt integrate -e tdb2 --loc --db-keep mystores/mydata file.ttl spo.rq`

By default, `rpt integrate` uses the in-memory engine. The `--engine` (short `-e`) option allows choosing a different RDF engine. For engines that require a file or a database folder, the location can be uniformly specified with `--db-loc` (short `--loc`). By default, **RPT will by default delete data it created itself but it will never delete existing data**. The flag `--db-keep` instructs RPT to keep database it created after termination.



### Example 5: SPARQL Proxy

You can quickly launch a SPARQL proxy with the combination of `-e` and `--server`:

`rpt integrate -e remote --loc https://dbpedia.org/sparql --server`

The proxy gives you a Yasgui frontend and the Linked Data Viewer.

Endpoints protected by basic authentication can be proxied by supplying the credentials with the URL:

`rpt integrate -e remote --loc https://USER:PWD@dbpedia.org/sparql --server`

Note, that this is **unsafe** and should be avoided in production, but it can be useful during development.


### Example 6: Indexing Spatial Data

Data that follows the GeoSPARQL standard can be indexed by providing the `--geoindex` option. The index will be automatically updated on the first query or update request that needs to access the data after any data modification.
Statements that intrinsically do not rely on the spatial index, namely `LOAD`, `INSERT DATA` and `DELETE DATA` mark the spatial index as potentially dirty but do not trigger immediate index recreation.

`rpt integrate --server --geoindex spatial-data.ttl`


## Embedded SPARQL Engines

Embedded SPARQL engines are built into RPT and thus readily available. The following engines are currently available:

<table>
    <tr><th>Engine</th><th>Description</th></tr>
    <tr><td><b>mem</b></td><td>The default in-memory engine based on Apache Jena. Data is discarded once the RPT process terminates.</td></tr>
    <tr><td><b>tdb2</b></td><td>Apache Jena's TDB2 persisent engine. Use <i>--loc</i> to specfify the database folder.</td></tr>
    <tr><td><b>binsearch</b></td><td>Binary search engine that operates directly on sorted N-Triples files. Use <i>--loc</i> to specify the file path or HTTP(s) URL to the N-Triples file. For URLs, HTTP range requests must be supported!</td></tr>
    <tr><td><b>remote</b></td><td>A pseudo engine that forwards all processing to the SPARQL endpoint whole URL is specified in <i>--loc</i>.</td></tr>
</table>



### (ARQ) Engine Configuration

The engines `mem`, `tdb2` and `binsearch` build an Jena's query engine `ARQ` and thus respect its configuration.

`rpt integrate  --set 'arq:queryTimeout=60000' myQuery.rq`




