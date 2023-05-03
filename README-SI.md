
# Sparql-Integrate
Integrate heterogeneous data with **standard SPARQL syntax** plus function extensions.


* Uses the plugin system of [Apache Jena](http://jena.apache.org/)'s SPARQL engine (ARQ) for adding the functionality to access local and remote data and to process JSON, CSV and XML formats
* Introduces *SPARQL functions* which compute a single RDF term from its arguments, e.g. `json json:parse(string)`
* ... and *SPARQL property functions* (can be seen as magic RDF properties) which transform literals into multiple SPARQL result set rows, e.g. `{ ?jsonLiteral json:unnest ?itemVar }`
* The concept of this tool is to be a swiss army knife for realizing small to medium sized data integration workflows as a mere sequence of SPARQL queries stored in a `.sparql` file
  * Note, that remote SPARQL queries can be performed using standard SPARQL simple federation


## Sparql Engines

Sparql integrate is a command line front end to different SPARQL engines of which some are embedded and can thus be used without having to launch a separate server.
A basic abstraction over the engines is provided by means of `--db-location` attribute which allows for referring to e.g. a name, directory, file or URL.

`rpt integrate --db-engine $ENGINE --db-location $LOCATION --db-loader $LOADER`

| Engine | Description                               |
|--------|-------------------------------------------|
| mem    | Jena's default ARQ engine (in-memory)     |
| tdb2   | Jena's tdb2 engine (disk-based)           |
| remote | Use any remote SPARQL endpoint            |
| difs   | Dataset in FileSystem engine              |


| Loader  | Description                                                                      |
|---------|----------------------------------------------------------------------------------|
| default | Use the engine's default loader                                                  |
| sansa   | Attempts to read RDF files in parallel and fires concurrent INSERT DATA requests |




## Function Reference and Programmatic Usage
This tool is just a thin command line wrapper for Jena ARQ and our extensions.

You can make the extensions available in your own Java/Scala project simply by adding a dependency to the
[jena-sparql-api sparql extensions module](https://github.com/SmartDataAnalytics/jena-sparql-api/tree/master/jena-sparql-api-sparql-ext).
This will auto-register all configuration-free extensions. Some extensions, such as HTTP, allow you to provide your own HTTPCient object (which enables intercepting HTTP requests for e.g. statistics and throttling), so you need to configure this yourself to your liking.


For the reference of supported SPARQL function extensions, please refer to the documentation of the [jena-sparql-api sparql extensions module](https://github.com/SmartDataAnalytics/jena-sparql-api/tree/master/jena-sparql-api-sparql-ext). 


## Install Latest Release
Installing as root will perform global install in the folders `/usr/local/share/sparql-integrate` and `/usr/local/bin`.
For non-root users, the folders are `~/Downloads/sparql-integrate` and `~/bin`.
Run `setup-latest-release.sh uninstall` to conveniently remove downloaded and generated files.

* via curl

    `bash -c "$(curl -fsSL https://raw.githubusercontent.com/SmartDataAnalytics/SparqlIntegrate/develop/setup-latest-release.sh)"`

* via wget

    `bash -c "$(wget -O- https://raw.githubusercontent.com/SmartDataAnalytics/SparqlIntegrate/develop/setup-latest-release.sh)"`

## Usage Example

The most convenient way to use this tool is to build this java project and make it available via a command line argument.
The build also creates a debian package for convenient installation on debian-like systems (such as Ubuntu).


```bash
sparql-integrate [--server] file1.sparql ... filen.sparql
```


```sparql
#example.sparql
#==============

PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#>
PREFIX q: <http://qrowd-project.eu/ontology/>
CONSTRUCT {
  ?s
    a q:BikeStation ;
    q:id ?id ;
    rdfs:label ?name ;
    wgs:long ?x ;
    wgs:lat ?y ;
    .
}
{
  # url:text is a property function that fetches the content of subject URL and
  # makes it available as a SPARQL result set row via the object variable
  <https://raw.githubusercontent.com/QROWD/QROWD-RDF-Data-Integration/master/datasets/1014-electric-bikesharing-stations/trento-bike-sharing.json> url:text ?src .
  # Treat the url text as a json object
  BIND(STRDT(?src, xsd:json) AS ?json) .

  # Unnest each item of the json array into its own SPARQL result set row
  # (The parenthesis around ?i are currently needed; we will try to get rid of them in a later version)
  ?json json:unnest (?i) .

  # For each row, craft the values for the CONSTRUCT template
  BIND("http://qrowd-project.eu/resource/" AS ?ns)

  BIND(json:path(?i, "$.id") AS ?id)
  BIND(URI(CONCAT(?ns, ENCODE_FOR_URI(?id))) AS ?s)
  BIND(json:path(?i, "$.name") AS ?name)
  BIND(json:path(?i, "$.position[0]") AS ?x)
  BIND(json:path(?i, "$.position[1]") AS ?y)
}
```

## Namespaces
Several common namespaces are readily available:

* Jena's Extended namespaces: `rdf`, `rdfs`, `owl`, `dc`, `xsd`, `rss`, `vcard`, `ja`, `eg`.
* All namespaces of the [RDFa Initial Context]( https://www.w3.org/2011/rdfa-context/rdfa-1.1).
* Additionally, the namespaces `json`, `csv`, `xml` and `url` are introduced, which contain the SPARQL extensions.

## Environment variables
sparql-integrate provides the following SPARQL extensions for passing values to queries:

### sys:getenv
* The function `sys:getenv`: It can be used in expressions such as `BIND(sys:getenv('VAR_NAME') AS ?x)`


### `<env:...>` URIS

Substitution takes place for explicit mentions of URIs that start with 'env:'.
The rules are as follows:
* `<env://FOOBAR>` treats the value of the environment variable FOOBAR as a _URI_. This can be used e.g. in conjunction with the `SERVICE` keyword: `SERVICE <env:REMOTE_URL> { }`
* `<env:FOOBAR>` (without //) substitues the URL with a _string literal_

If there is no value defined for a key, then the behavior is not yet defined - in construct templates, such keys should be treated as unbound.
In query patterns, it should yield an empty string literal or URI that does not appear in the data.


## Detailed Usage

### The .sparql file
This is really just a file containing SPARQL queries with no extra syntactic fuzz.
The following rules apply:

* By default, sparql-integrate runs all queries run against an empty in memory SPARQL dataset.
  * SPARQL Update queries (INSERT/DELETE) will modify this dataset
  * `CONSTRUCT` queries are used to produce actual data output to STDOUT as NTRIPLES
  * `SELECT` queries will output formatted tables on STDERR and can be used for debugging / information
* Relative IRIs of SPARQL queries are resolved against the folder of the containing `.sparql` file. 
Hence, a data integration project can just put a `.sparql` file next to data files, such as `mydata.json`, and a query can reference it with `SELECT * { <mydata.json> url:text ?src }`.

### Command Line Options

Most arguments are processed in the order in which they appear.

* `file.sparql` Specify one ore more SPARQL query files which to run against an in-memory triple store.
* `file.ttl` RDF files are loaded into the in-memory triple store.
* `--w` Select the output format based on Jena's registry, such as `--w=trig/pretty`. Defaults to the nquads format.
* `--server` Start a local SPARQL endpoint, featuring a simple [SNORQL HTML](https://github.com/kurtjx/SNORQL) frontend, for exploring the content of the default dataset.
* `--cwd=directory` Sets the base URL (and current working directory) for any subsequent files. Can be used multiple times: `sparql-integrate --cwd=/tmp file1.ttl --cwd=subfolder file2.sparql`
* `--cwd` (without argument) Resets base URL and cwd to whatever its initial value was
* `<(echo 'SELECT * { ?s ?p ?o }')` If you want to specify queries directly (without files), use this bash `<(..)` trick which substitutes the expression with a temporary file that exists for the duration of the process.

These arguments are global:

* `--u` Activate union default graph mode: The set of named graphs becomes the default graph. Useful for running "named-graph-unaware" SPARQL queries on quad-based datasets, such as data from `.trig` files. Internally, triple patterns `s p o` are rewritten to `GRAPH g { s p o }`.



## Related projects

* [TARQL](https://github.com/tarql/tarql)
* [JARQL](https://github.com/linked-solutions/jarql)
* [RML](http://rml.io)
* [SPARQLGenerate](http://w3id.org/sparql-generate)

