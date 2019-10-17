#!/bin/bash
sparql-integrate --io=iotest.nt <(echo 'INSERT { eg:a eg:b ?c } WHERE { { SELECT (COUNT(*) AS ?c) { ?s ?p ?o } } }') spo.sparql

