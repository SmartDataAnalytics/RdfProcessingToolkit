#!/bin/bash

time ngs map --sparql even.sparql test-data.trig > /tmp/ngs-bench-map.trig

# Note: --u for union default graph mode not needed as the query is graph aware
# Note --w=trig/pretty is too slow - see https://issues.apache.org/jira/browse/JENA-1848
# time sparql-integrate --w=trig/pretty test-data.trig even.sparql > /tmp/ngs-bench-si.trig
time sparql-integrate test-data.trig even.sparql > /tmp/ngs-bench-si.trig
