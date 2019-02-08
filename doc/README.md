# SPARQL Integrate examples



## Retrieving remote content
A simple but effective mechanism for unified retrieval of local or remote data is provided by the `url:text` function and property function.

```
SELECT * {
  <example-data/data.csv> url:text ?str
}
```

```
SELECT * {
  # TODO: Insert url to data.csv via a rawgit-like service
  <https://cdn.jsdelivr.net/gh/...> url:text ?str
}
```


```
--------------------------------
| str                          |
================================
| "\"a\",\"b\"\n\"c\",\"d\"\n" |
--------------------------------
```

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
JSON arrays can be tranformed into result sets using the `json:unnest` function.

```
SELECT * {
  '[true, 1, "hi", {}, []]'^^xsd:json json:unnest (?item ?index)
}
```

This operation transform literal values (boolean, string and numeric) into corresponding standard RDF literals, whereas JSON objects (arrays *are* also JSON objects) remain literals of type `xsd:json`.

**Note: In a future version we also intend to allow `?json json:unnest ?item` in cases where the index is not needed, but this is not supported yet.**
 
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

One can also specify a specific index:
```
SELECT * {
  '[true, 1, "hi", {}, []]'^^xsd:json json:unnest (?item 1)
}

```

```
--------
| item |
========
| "hi" |
--------
```

* Zipping JSON arrays
Here we show a more sophicisticated example, that combines several techniques:

* Multiple dependent SPARQL statements: The SELECT query refers to the inserted data
* Construction of IRIs

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
  BIND(IRI(CONCAT(?ns, ENCODE_FOR_URI(?id))) AS ?s)
}
```


```
------------------------------------------
| s                            | name    |
==========================================
| <http://www.example.org/id1> | "name1" |
| <http://www.example.org/id2> | "name2" |
------------------------------------------
```


## Processing CSV
In general, the `csv:parse` property function is used to make CSV data available in a SPARQL query with each CSV row becoming an entry in the result set. The syntax is `?s csv:parse(?rowJson "optionsString)"`.
The options string is composed of a base CSV format name followed by an optional list of modifiers.

If the subject is a string literal, it is interpreted as CSV data.
If the argument is an IRI, `sparql-integrate` will attempt to resolve the IRI to an input stream to the data. If no further joins are used, this is a **streaming** operation that works with arbitrary large CSV files.

**NOTE ?rowJson must be variable**


* Using the first row as headers

The `-h` option causes the first row to be used as headers which means that the header values will be used as keys in the JSON document for each row.

```
SELECT * {
"""fn,ln
Mary,Major
John,Doe""" csv:parse (?rowJson "excel -h")
}
```


```
SELECT * { <example-data/people.csv> csv:parse (?rowJson "excel -h") }
```

```
---------------------------------------------------------------------------------
| rowJson                                                                       |
=================================================================================
| "{\"fn\":\"Mary\",\"ln\":\"Major\"}"^^<http://www.w3.org/2001/XMLSchema#json> |
| "{\"fn\":\"John\",\"ln\":\"Doe\"}"^^<http://www.w3.org/2001/XMLSchema#json>   |
---------------------------------------------------------------------------------

```



## Processing XML
For XML processing, the `xml:path` function and property functions are provided.
Unlike JSON, XML does not have a native array datatype which could be used for storing XML nodes matching an XPath expression.
In order to avoid having to introduce one, the `xml:path` *property function* can be used to unnest XML nodes based on an XPath expression, whereas the `xml:path` *function* can be used to access attributes and texts:

**NOTE: XPath matches are converted to independent RDF terms, i.e. the link to a match's parent element is lost and so is e.g. namespace information defined on ancestors. In most cases, XPath's `local-name` function should be usable as a workaround: `//[local-name()="elementNameWithoutNamespace"]`**

```
SELECT * {
  BIND('<ul id="ul1"><li>item</li></ul>'^^xsd:xml AS ?xml)
  BIND(xml:path(?xml, "//ul/@id") AS ?id)
  BIND(xml:path(?xml, "//li") AS ?item)
}
```

```
------------------------------------------------------------------------------------------------
| xml                                                                         | id    | item   |
================================================================================================
| "<ul id=\"ul1\"><li>item</li></ul>"^^<http://www.w3.org/2001/XMLSchema#xml> | "ul1" | "item" |
------------------------------------------------------------------------------------------------

```

Unnesting an XML document is done in regard to a given xpath expression:
```
SELECT * {
  """<ul id="ul1"><li>item</li></ul>"""^^xsd:xml xml:unnest ("//li" ?item)
}
```

```
-----------------------------------------------------------------------------------------------------------------------
| item                                                                                                                |
=======================================================================================================================
| "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><li>item</li>"^^<http://www.w3.org/2001/XMLSchema#xml> |
-----------------------------------------------------------------------------------------------------------------------
```


## Querying the file system

* Simple recursive listing of files
(fs-list-files.sparql)[fs-list-files.sparql]

```
SELECT * {
  <example-data> fs:find ?file
}
```

The output will use absolute file URLs.
**NOTE: This behavior is not stable and may be changed to relative file URLs**
```
---------------------------------------
| file                                |
=======================================
| <file:///.../example-data/data.ttl> |
| <file:///.../example-data/data.csv> |
---------------------------------------
```

* Filtering files by content
The SPARQL predicate `fs:probeRdf` yields true if the given RDF URL argument refers to an existing file whose content can be parsed as RDF.


```
SELECT * {
  <example-data> fs:find ?file
  FILTER(fs:probeRdf(?file))
}

```

```
---------------------------------------
| file                                |
=======================================
| <file:///.../example-data/data.ttl> |
---------------------------------------
```


* Querying over files

`sparql-integrate` ships with an enhancement of jena's query processor that allows "federating" queries to files via the `SERVICE` clause:

```
SELECT * {
  <example-data> fs:find ?file
  FILTER(fs:probeRdf(?file))
  SERVICE ?file {
    ?s ?p ?o
  }
}
```

**NOTE: As of now, the fs:probeRdf check is necessary, as querying non-rdf files will raise an exception. This behavior is subject to change.**

```
------------------------------------------------------------------------------------------------------------------------------
| file                                | s                          | p                          | o                          |
==============================================================================================================================
| <file:///.../example-data/data.ttl> | <http://www.example.org/s> | <http://www.example.org/p> | <http://www.example.org/o> |
------------------------------------------------------------------------------------------------------------------------------
```



