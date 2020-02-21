## Named Graph Streams (NGS)

The command line swiss army knife for efficient processing of collections of named graphs using standard turtle syntaxes (n-quads, trig).
The tool suite is akin to well know shell scripting tools, such as head, tail, join, sort (including sort --merge), etc.

Named graphs can be used to denote data records, i.e. sets of triples that form a logical unit for the task at hand.

### Implementation status

| Command | Descripion                                                                       | Status |
|---------|----------------------------------------------------------------------------------|--------|
| head    | Output the first n graphs                                                        | done   |
| map     | Execute a SPARQL query on every named graph and yield the set of obtained graphs | done   |
| join    | Merge triples of named graphs based on a key                                     | wip    |
| sort    | Sort named graphs based on a key                                                 | wip    |
| ...     | More to come                                                                     |        |



### Example Usage


* Output the first n named graphs of a trig file
* * `cat data.trig | ngs head`
* * `cat data.trig | ngs head -n 1000` (Get the first 1000 graphs)
* * `ngs head data.trig` (without useless cat)
* Run a SPARQL query (from a file) on each graph. This is run in parallel for high efficiency.
* * `cat data.trig | ngs map --sparql script.sparql`
* Combine both steps
* * `cat data.trig | ngs head | ngs map --sparql script.sparql`

### Rules

* Input must be quads where all quads of a graph must be consecutive
* * TODO Provide a command that efficiently sorts quads by graph
* `map`: Map an input named graph to a set of output graphs. Output graphs are created using CONSTRUCT queries. Thanks to Jena's extension, CONSTRUCT { GRAPH ?g { ... } } is allowed. All explicitly generated named graphs are returned as-is. CONSTRUCT'ed data in the default graph is wrapped in a graph with the same name as the input graph.

### Performance Metrics
* Running a single query over the whole dataset vs parallel map - TBD
