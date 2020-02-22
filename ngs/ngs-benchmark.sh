#zsh time output looks better imo
#/bin/bash

data="$1"
query="$2"

time ngs map --sparql "$query" "$data" > /tmp/ngs-bench-map.trig

# Note: --u for union default graph mode not needed as the query is graph aware
# Note --w=trig/pretty is too slow - see https://issues.apache.org/jira/browse/JENA-1848
# time sparql-integrate --w=trig/pretty test-data.trig even.sparql > /tmp/ngs-bench-si.trig
time sparql-integrate "$data" "$query" > /tmp/ngs-bench-si.trig
