## GraphQL to SPARQL Rewriter


### Features

* **Fully streaming** JSON generation from SPARQL result sets.
* Rewrites GraphQL to a single **SPARQL query**.
* Vendor-independent: The generated SPARQL queries can run on any SPARQL 1.1 \* endpoint.
* Self-contained queries
    * No need for expensive schema generation or data summarization
    * No need to manage additional mapping files
* 


### Limitations and Pitfalls

* The generated SPARQL query makes use of the LATERAL feature, however this can be polyfilled at the cost of multiple requests with jenax SPARQL polyfills in jenax-dataaccess
* The GraphQL-to-SPARQL rewriter makes the following assumptions:
    * Inter-UNION order preservation: Given graph patterns A, B, C, then it is expected that `UNION(A, UNION(B, C))` yields all bindings of A before B, and all bindings of B before C.
    * Intra-UNION order preservation: ORDER BY clauses within a union must be preserved.


### Core Concepts

A GraphQL query is specified by a GraphQL *document* which contains one *query operation definiton*. A *query operation definition* is primarily composed of *fields*, which can have *arguments*, carry annotations called *directives*.


### Mapping SPARQL to JSON

Each GraphQL field is associated with the following aspects:
* a SPARQL pattern
    * of which one list of variables act as the *source* and
    * another list of variables that act as the *target*.


```
{

}
```



# Reference





