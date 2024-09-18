# RDF Processing Toolkit (RPT)

RPT makes RDF/SPARQL workflows on the command line easy. The RDF Processing Toolkit (RPT) integrates several of our tools into a single CLI frontend: It features commands for running SPARQL-statements on triple and quad based data both streaming and static. SPARQL extensions for working with CSV, JSON and XML are included. So is an RML toolkit that allows one to convert RML to SPARQL (or TARQL). RPT embeds several SPARQL engines, including Jena’s ARQ and TDB, as well as one of our own for SPARQL-based batch processing using Apache Spark.

## News

* 2024-09-18 Improved [documentation](https://smartdataanalytics.github.io/RdfProcessingToolkit/)!

[Previous entries](#History)


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

* 2023-05-19 New quality of life features: `cpcat` command and the canned queries `tree.rq` and `gtree.rq`.
* 2023-04-04 Release v1.9.5! RPT now ships with `sansa` (Apache Spark based tooling) and `rmltk` (RML Toolkit) features. A proper GitHub release will follow once Apache Jena 4.8.0 is out as some code depends on its latest SNAPSHOT changes.
* 2023-03-28 Started updating documentation to latest changes (ongoing)

