## Named Graph Streams (NGS)

```bash
➜  ngs -h
Usage: ngs [-h] [COMMAND]
Named Graph Stream Subcommands
  -h, --help
Commands:
  cat       Output and optionally convert graph input
  filter    Yield items (not) satisfying a given condition
  head      List or skip the first n named graphs
  tail      Output the last named graphs
  map       (flat-)Map each named graph to a new set of named graphs
  probe     Determine content type based on input
  sort      Sort named graphs by key
  subjects  Group triples with consecutive subjects into named graphs
  until     Yield items up to and including the first one that satisfies the condition
  wc        Mimicks the wordcount (wc) command; counts graphs or quads
  while     Yield items up to but excluding the one that satisfies the condition
```


The command line swiss army knife for efficient processing of collections of named graphs using standard quad-based RDF syntaxes (n-quads, trig).
The tool suite is akin to well know shell scripting tools, such as head, tail, join, sort (including sort --merge), etc.

Named graphs can be used to denote data records, i.e. sets of triples that form a logical unit for the task at hand.

### Building
NGS is part of the rdf-processing-toolkit build. Installing the debian package also makes the command `ngs` tool available.


### Example Usage

* Output the first n named graphs of a trig file
* * `cat data.trig | ngs head`
* * `cat data.trig | ngs head -n 1000` (Get the first 1000 graphs)
* * `ngs head data.trig` (without useless cat)
* Run a SPARQL query (from a file or inline) on each graph. This is run in parallel for high efficiency.
* * `cat data.trig | ngs map --sparql script.sparql`
* Given some set of initial graphs, map them through a transformation, sort the resulting graphs by some key (merging consecutive ones together), sort the result randomly and take a sample of 1000
* * `cat data.trig | ngs map --sparql script.sparql | ngs sort -m -k '?o { ?s <someProb> ?o }' | ngs sort -m -R | ngs head -n 1000`


### Notes on ngs map

* When using `ngs map` with SPARQL queries that use a SERVICE clause the connection and query execution timeouts can be controlled using the `-t` flag:
```
echo "<http://dbpedia.org/resource/Leipzig> <x> <y>" | ngs subjects |\
ngs map -t '5000,5000' --sparql 'SELECT DISTINCT ?t { ?s ?p ?o SERVICE <http://dbpedia.org/sparql> { ?s a ?t } }'
```
* Triples and quads can be mapped into the default graph or a specific named graph using the following variants
```
cat file.trig | ngs map --dg
ngs map --dg file.trig
ngs map --graph 'http://my.graph/' file.ttl
```

### Rules

* Input must be quads where all quads of a graph must be consecutive
* `map`: Map an input named graph to a set of output graphs. Output graphs are created using CONSTRUCT queries. Thanks to Jena's extension, CONSTRUCT { GRAPH ?g { ... } } is allowed. All explicitly generated named graphs are returned as-is. CONSTRUCT'ed data in the default graph is wrapped in a graph with the same name as the input graph.



### Implementation status
* Note: head and tail currently do not support the + flag as in e.g. ngs head -n+10

| Command    | Descripion                                                                       | Status |
|------------|----------------------------------------------------------------------------------|--------|
| head       | Output the first n graphs                                                        | done   |
| cat        | Output all graphs                                                                | done   |
| probe      | Probe a file or stdin for a quad-based format                                    | done   |
| map        | Execute a SPARQL query on every named graph and yield the set of obtained graphs | done   |
| until      | Yield graphs up to and including the first one that satisfies a condition        | done   |
| while      | Yield graphs as long as they satisfy a condition                                 | done   |
| wc         | Count graphs or quads                                                            | done   |
| join       | Merge triples of named graphs based on a key                                     | wip    |
| sort       | Sort named graphs based on a key                                                 | done   |
| subjects   | Group a stream of triples by subject to yield a stream of named graphs           | done   |
| filterKeep | Only yield graphs for which an ASK query yields true                             | wip    |
| filterDrop | Omit graphs from output when an ASK query yields true                            | wip    |
| ...        | More to come                                                                     |        |


