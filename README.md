# Sparql-Integrate
Integrate heterogeneous data with **standard SPARQL syntax** plus function extensions.


* Uses the plugin system of [Apache Jena](http://jena.apache.org/)'s SPARQL engine (ARQ) for adding the functionality to access local and remote data and to process JSON, CSV and XML formats
* Introduces *SPARQL functions* which compute a single RDF term from its arguments, e.g. `json json:parse(string)`
* ... and *SPARQL property functions* (can be seen as magic RDF properties) which transform literals into multiple SPARQL result set rows, e.g. `{ ?jsonLiteral json:unnest ?itemVar }`
* The concept of this tool is to be a swiss army knife for realizing small to medium sized data integration workflows as a mere sequence of SPARQL queries stored in a `.sparql` file
  * Note, that remote SPARQL queries can be performed using standard SPARQL simple federation


## Function Reference and Programmatic Usage
This tool is just a thin command line wrapper for Jena ARQ and our extensions.

You can make the extensions available in your own Java/Scala project simply by adding a dependency to the
[jena-sparql-api sparql extensions module](https://github.com/SmartDataAnalytics/jena-sparql-api/tree/master/jena-sparql-api-sparql-ext).
This will auto-register all configuration-free extensions. Some extensions, such as HTTP, allow you to provide your own HTTPCient object (which enables intercepting HTTP requests for e.g. statistics and throttling), so you need to configure this yourself to your liking.


For the reference of supported SPARQL function extensions, please refer to the documentation of the [jena-sparql-api sparql extensions module](https://github.com/SmartDataAnalytics/jena-sparql-api/tree/master/jena-sparql-api-sparql-ext). 


## Usage Example

The most convenient way to use this tool is to build this java project and make it available via a command line argument.
The build also creates a debian package for convenient installation on debian-like systems (such as Ubuntu).


```bash
sparql-generate [--server] file1.sparql ... filen.sparql
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
  BIND(json:parse(?src) AS ?json) .

  # Unnest each item of the json array into its own SPARQL result set row
  ?json json:unnest ?i .

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
  * We have plans to enhance this to all [RDFa Initial Context Namespaces]( https://www.w3.org/2011/rdfa-context/rdfa-1.1).
* Additionally, the namespaces `json`, `csv`, `xml` and `url` are introduced, which contain the SPARQL extensions.

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

* `file.sparql` Specify one ore more SPARQL query files which to run against the default dataset.
* `--server` Start a local SPARQL endpoint, featuring a simple [SNORQL HTML](https://github.com/kurtjx/SNORQL) frontend, for exploring the content of the default dataset.

## Building
The build requires maven. 

```bash
mvn clean install
```

Installing the Debian packages can then be easily accomplished using:
```
sudo dpkg -i $(find . -name "sparql-integrate*.deb")
```

You can also manually start the tool from the 'sparql-integrate-cli/target` folder using:
```bash
java -cp ".:lib/*" "-Dloader.main=org.aksw.sparql_integrate.cli.MainSparqlIntegrateCli" "org.springframework.boot.loader.PropertiesLauncher" "your" "args"
```

## Related projects

* [TARQL](https://github.com/tarql/tarql)
* [JARQL](https://github.com/linked-solutions/jarql)
* [RML](http://rml.io)
* [SPARQLGenerate](http://w3id.org/sparql-generate)

## Acknowledgements

* This project is developed with funding from the [QROWD](http://qrowd-project.eu/) H2020 project. Visit the [QROWD GitHub Organization](https://github.com/Qrowd) for more Open Source tools!

