#!/usr/bin/env bats

setup() {
    SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_DIRNAME")" && pwd)"
    RESOURCES_DIR="$SCRIPT_DIR/resources"
    TEMP_DIR=$(mktemp -d)
}

@test "tdb2: load turtle data and output turtle" {
    run rpt integrate -e tdb2 --loc "$TEMP_DIR/tdb2-db" "$RESOURCES_DIR/input-triples.ttl" "$RESOURCES_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.ttl" --db-keep
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.ttl" ]
    
    expected=$(cat "$RESOURCES_DIR/expected-turtle.ttl")
    actual=$(cat "$TEMP_DIR/output.ttl")
    [ "$expected" = "$actual" ]
}

@test "tdb2: load turtle data and output ntriples" {
    run rpt integrate -e tdb2 --loc "$TEMP_DIR/tdb2-db" "$RESOURCES_DIR/input-triples.ttl" "$RESOURCES_DIR/query-construct-triples.sparql" -o "$TEMP_DIR/output.nt" --db-keep
    [ "$status" -eq 0 ]
    [ -f "$TEMP_DIR/output.nt" ]
    
    expected=$(cat "$RESOURCES_DIR/expected-ntriples.nt")
    actual=$(cat "$TEMP_DIR/output.nt")
    [ "$expected" = "$actual" ]
}

teardown() {
    rm -rf "$TEMP_DIR"
}
