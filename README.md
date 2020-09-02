# RDF Processing Toolkit

RDF/SPARQL Workflows on the Command Line made easy. The toolkit provides the following commands for running SPARQL-queries on triple and quad based data

* `sparql-integrate`: Ad-hoc querying and transformation of datasets featuring SPARQL-extensions for CSV, XML and JSON processing and JSON output that allows for building bash pipes in a breeze
* `ngs`: Process collections of named graphs in streaming fashion. Process huge datasets without running into memory issues.


## Example Use Cases

* [Lodservatory](https://github.com/SmartDataAnalytics/lodservatory) implements SPARQL endpoint monitoring uses these tools in this [script](https://github.com/SmartDataAnalytics/lodservatory/blob/master/update-status.sh) called from this [git action](https://github.com/SmartDataAnalytics/lodservatory/blob/master/.github/workflows/main.yml).
* [Linked Sparql Queries](https://github.com/AKSW/LSQ) provides tools to RDFize SPARQL query logs and run benchmark on the resulting RDF. The triples related to a query represent an instance of a sophisticated domain model and are grouped in a named graph. Depending on the input size one can end up with millions of named graphs describing queries amounting to billions of triples. With ngs one can easily extract complete samples of the queries' models without a related triple being left behind.


## Example Usage

* `sparql-integrate` allows one to load multiple RDF files and run multiple queries on them in a single invocation. Further prefixes from a snapshot of [prefix.cc](https://prefix.cc) are predefined and we made the SELECT keyword of SPARQL optional in order to make scripting less verbose. The `--jq` flag enables JSON output for interoperability with the conventional jq tool


```
sparql-integrate loadFile.rdf update.sparql loadAnotherFile.rdf query.sparql

sparql-integrate --jq file.ttl '?s { ?s a foaf:Person }' | jq '.[].s'
```


* `ngs` is your well known bash tooling such as `head`, `tail`, `wc` adapted to named graphs instead of lines of text
```
# Group RDF into graph based on consecutive subjects and for each named graph count the number of triples
cat file.ttl | ngs subjects | ngs map --sparql 'CONSTRUCT { ?s eg:triples ?c} { SELECT ?s COUNT(*) { ?s ?p ?o } GROUP ?s }

# Count number of named graphs
ngs wc file.trig

# Output the first 3 graphs produced by another command
./produce-graphs.sh | ngs head -n 3
```

## Detailed Documentation

* [sparql-integrate](README-SI.md)
* [ngs](README-NGS.md)


## License
The source code of this repo is published under the [Apache License Version 2.0](LICENSE).
Dependencies may be licensed under different terms. When in doubt please refer to the licenses of the dependencies declared in the pom.xml files.

## Related projects

* [TARQL](https://github.com/tarql/tarql)
* [JARQL](https://github.com/linked-solutions/jarql)
* [RML](http://rml.io)
* [SPARQLGenerate](http://w3id.org/sparql-generate)

## Acknowledgements

* This project is developed with funding from the [QROWD](http://qrowd-project.eu/) H2020 project. Visit the [QROWD GitHub Organization](https://github.com/Qrowd) for more Open Source tools!

