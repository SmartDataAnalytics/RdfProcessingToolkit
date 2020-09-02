TODO Outdated values; We reported https://issues.apache.org/jira/browse/JENA-1862 and need to redo the eval...


### Performance Metrics

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

