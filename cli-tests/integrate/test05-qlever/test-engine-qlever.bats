#!/usr/bin/env bats

setup() {
    export TEST_DIR="/home/raven/Projects/Eclipse/rdf-processing-toolkit-parent/cli-tests/integrate/test05-qlever"
    export TEMP_DIR=$(mktemp -d)
}

@test "qlever: load turtle data and output turtle" {
    run rpt integrate -e qlever "$TEST_DIR/input-triples.ttl" "$TEST_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.ttl"
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.ttl" ]
    
    expected=$(cat "$TEST_DIR/expected-turtle.ttl")
    actual=$(cat "$TEMP_DIR/output.ttl")
    [ "$expected" = "$actual" ]
}

@test "qlever: load turtle data and output ntriples" {
    run rpt integrate -e qlever "$TEST_DIR/input-triples.ttl" "$TEST_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.nt"
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.nt" ]
    
    expected=$(cat "$TEST_DIR/expected-ntriples.nt")
    actual=$(cat "$TEMP_DIR/output.nt")
    [ "$expected" = "$actual" ]
}

teardown() {
    rm -rf "$TEMP_DIR"
}
