## Named Graph Streams (NGS)

The command line swiss army knife for efficient processing of collections of named graphs using standard turtle syntaxes (n-quads, trig).
The tool suite is akin to well know shell scripting tools, such as head, tail, join, sort (including sort --merge), etc.

Named graphs can be used to denote data records, i.e. sets of triples that form a logical unit for the task at hand.

### Building
NGS is part of the Sparql-Integrate build. Installing the debian package also makes the command `ngs` tool available.

TODO Setup a bundle github release process

### Implementation status

| Command | Descripion                                                                       | Status |
|---------|----------------------------------------------------------------------------------|--------|
| head    | Output the first n graphs                                                        | done   |
| cat     | Output all graphs                                                                | done   |
| probe   | Probe a file or stdin for a quad-based format                                    | done   |
| map     | Execute a SPARQL query on every named graph and yield the set of obtained graphs | done   |
| until   | Yield graphs up to and including the first one that satisfies a condition        | done   |
| while   | Yield graphs as long as they satisfy a condition                                 | done   |
| wc      | Count graphs or quads                                                            | done   |
| join    | Merge triples of named graphs based on a key                                     | wip    |
| sort    | Sort named graphs based on a key                                                 | done   |
| ...     | More to come                                                                     |        |



### Example Usage


* Output the first n named graphs of a trig file
* * `cat data.trig | ngs head`
* * `cat data.trig | ngs head -n 1000` (Get the first 1000 graphs)
* * `ngs head data.trig` (without useless cat)
* Run a SPARQL query (from a file) on each graph. This is run in parallel for high efficiency.
* * `cat data.trig | ngs map --sparql script.sparql`
* Given some set of initial graphs, map then through a transformation, sort the resulting graphs by some key (merging consecutive ones together), sort the result randomly and take a sample of 1000
* * `cat data.trig | ngs map --sparql script.sparql | ngs sort -m -k '?o { ?s <someProb> ?o }' | ngs sort -m -R | ngs head -n 1000`

### Rules

* Input must be quads where all quads of a graph must be consecutive
* * TODO Provide a command that efficiently sorts quads by graph
* `map`: Map an input named graph to a set of output graphs. Output graphs are created using CONSTRUCT queries. Thanks to Jena's extension, CONSTRUCT { GRAPH ?g { ... } } is allowed. All explicitly generated named graphs are returned as-is. CONSTRUCT'ed data in the default graph is wrapped in a graph with the same name as the input graph.

### Performance Metrics
TODO Outdated values

The `ngs` folder contains a small benchmark utility:

```
# Create a given number of graphs
./ngs-create-test-data.sh 1000000 > test-data.trig

# The benchmark task is to emit all named graphs containing an even number
./ngs-benchmark.sh
```


* Running a single query over the whole dataset vs parallel map

Notebook, 2 cores + ht

even.sparql
<pre>
ngs-map          165.54user 6.56system 0:53.02elapsed 324%CPU
sparql-integrate 139.77user 1.82system 0:51.12elapsed 276%CPU
</pre>


hash.sparql
<pre>
ngs-map          182.86user 7.20system 0:57.41elapsed 331%CPU
sparql-integrate 155.86user 2.03system 0:58.42elapsed 270%CPU
</pre>


Server:



