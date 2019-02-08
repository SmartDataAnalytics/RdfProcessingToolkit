# SPARQL Integrate examples


## Processing JSON
This section explains the supported features for working with JSON literals in SPARQL. For simplicity, we hijacked the `xsd` namespace and
added a `xsd:json` datatype.

* Creating a JSON literal
```
SELECT * {
  BIND('{"hello": "world"}'^^xsd:json AS ?s)
}
```

Alternatively, in case you need to create a JSON literal from a variable, you can use the standard
SPARQL function *STRDT*:
```
SELECT ?s {
  BIND('{"hello": "world"}' AS ?str)
  BIND(STRDT(?str, xsd:json) AS ?s)
}
```

In both cases the output is:
```
---------------------------------------------------------------------
| s                                                                 |
=====================================================================
| "{\"hello\": \"world\"}"^^<http://www.w3.org/2001/XMLSchema#json> |
---------------------------------------------------------------------
```

* Extracting a JSON attribute
The main function for extracting attributes from a JSON object is `json:path`:
```
SELECT ?s {
  BIND('{"hello": "world"}'^^xsd:json AS ?str)
  BIND(json:path(?str, "$.hello") AS ?s)
}
```
Of course, inlining is possible
```
SELECT ?s {
  BIND(json:path('{"hello": "world"}'^^xsd:json, "$.hello") AS ?s)
}
```

Again, in both cases the output is:
```
-----------
| s       |
===========
| "world" |
-----------
```

* Unnesting JSON arrays
JSON arrays can be tranformed into result sets using the `json:unnest` function:

```
SELECT * {
  '[true, 1, "hi", {}, []]'^^xsd:json json:unnest (?item ?index)
}

```


Note, that this operation transform literal values (boolean, string and numeric) into corresponding standard RDF literals, whereas JSON objects (arrays *are* also JSON objects) remain literals of type `xsd:json`.
 
```
------------------------------------------------------------
| item                                             | index |
============================================================
| true                                             | 0     |
| "1.0"^^<http://www.w3.org/2001/XMLSchema#double> | 1     |
| "hi"                                             | 2     |
| "{}"^^<http://www.w3.org/2001/XMLSchema#json>    | 3     |
| "[]"^^<http://www.w3.org/2001/XMLSchema#json>    | 4     |
------------------------------------------------------------
```


* Zipping JSON arrays

```
# Insert an example resource with a JSON literal
INSERT DATA {
  eg:workload1 eg:workload """{
    "ids": [ "id1", "id2"],
    "names": [ "name1", "name2", ]
  }"""^^xsd:json
}


# Combine id and name
SELECT ?s ?name
WHERE {
  ?x eg:workload ?o  .
  BIND(json:path(?o, "$.ids") AS ?ids)
  BIND(json:path(?o, "$.names") AS ?names)

  ?ids json:unnest (?id ?i) .
  ?names json:unnest (?name ?i) .

  BIND("http://www.example.org/" AS ?ns)
  BIND(URI(CONCAT(?ns, 'stop-', ENCODE_FOR_URI(?id))) AS ?s)
}


```


## Processing CSV

```
```

## Querying the file system

### 
(fs-list-files.sparql)[fs-list-files.sparql]

```
SELECT ?file {
  <folder> fs:find ?file
}
```





