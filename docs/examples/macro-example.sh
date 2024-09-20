#!/bin/bash

rpt integrate --macro macros.ttl 'SELECT (eg:greet("John Doe") AS ?x) {}' --out-format tsv | tail -n1

