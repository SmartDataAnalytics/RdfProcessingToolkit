#!/bin/bash

MAX=${1:-10}

echo "@prefix eg: <http://www.example.org/> ."
for i in `seq 1 $MAX`; do
  echo "<urn:graph-$i> { <urn:s-$i> eg:idx $i }"
done

