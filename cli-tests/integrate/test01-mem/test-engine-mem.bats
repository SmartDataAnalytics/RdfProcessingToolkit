#!/usr/bin/env bats

setup() {
    SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_DIRNAME")" && pwd)"
    RESOURCES_DIR="$SCRIPT_DIR/resources"
    TEMP_DIR=$(mktemp -d)
}

@test "mem: load turtle data and output turtle" {
    run rpt integrate -e mem "$RESOURCES_DIR/input-triples.ttl" "$RESOURCES_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.ttl"
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.ttl" ]
    
    expected=$(cat "$RESOURCES_DIR/expected-turtle.ttl")
    actual=$(cat "$TEMP_DIR/output.ttl")
    [ "$expected" = "$actual" ]
}

@test "mem: load turtle data and output ntriples" {
    run rpt integrate -e mem "$RESOURCES_DIR/input-triples.ttl" "$RESOURCES_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.nt"
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.nt" ]
    
    expected=$(cat "$RESOURCES_DIR/expected-ntriples.nt")
    actual=$(cat "$TEMP_DIR/output.nt")
    [ "$expected" = "$actual" ]
}

@test "mem: load turtle data and output nquads" {
    run rpt integrate -e mem "$RESOURCES_DIR/input-triples.ttl" "$RESOURCES_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.nq"
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.nq" ]
    
    expected=$(cat "$RESOURCES_DIR/expected-nquads.nq")
    actual=$(cat "$TEMP_DIR/output.nq")
    [ "$expected" = "$actual" ]
}

@test "mem: load turtle data and output trig" {
    run rpt integrate -e mem "$RESOURCES_DIR/input-triples.ttl" "$RESOURCES_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.trig"
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.trig" ]
    
    expected=$(cat "$RESOURCES_DIR/expected-trig.trig")
    actual=$(cat "$TEMP_DIR/output.trig")
    [ "$expected" = "$actual" ]
}

teardown() {
    rm -rf "$TEMP_DIR"
}
