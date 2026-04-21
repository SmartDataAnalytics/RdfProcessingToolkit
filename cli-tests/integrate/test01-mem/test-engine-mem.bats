#!/usr/bin/env bats

setup() {
    TEST_DIR="$BATS_TEST_DIRNAME"
}

@test "mem: load turtle data and output turtle" {
    run rpt integrate -e mem "$TEST_DIR/input-triples.ttl" "$TEST_DIR/query-construct-triples.sparql" -o "$TEST_DIR/output.ttl"
    [ "$status" -eq 0 ]
    [ -f "$TEST_DIR/output.ttl" ]
    
    expected=$(cat "$TEST_DIR/expected-turtle.ttl")
    actual=$(cat "$TEST_DIR/output.ttl")
    [ "$expected" = "$actual" ]
}

@test "mem: load turtle data and output ntriples" {
    run rpt integrate -e mem "$TEST_DIR/input-triples.ttl" "$TEST_DIR/query-construct-triples.sparql" -o "$TEST_DIR/output.nt"
    [ "$status" -eq 0 ]
    [ -f "$TEST_DIR/output.nt" ]
    
    expected=$(cat "$TEST_DIR/expected-ntriples.nt")
    actual=$(cat "$TEST_DIR/output.nt")
    [ "$expected" = "$actual" ]
}

@test "mem: load turtle data and output nquads" {
    run rpt integrate -e mem "$TEST_DIR/input-triples.ttl" "$TEST_DIR/query-construct-triples.sparql" -o "$TEST_DIR/output.nq"
    [ "$status" -eq 0 ]
    [ -f "$TEST_DIR/output.nq" ]
    
    expected=$(cat "$TEST_DIR/expected-nquads.nq")
    actual=$(cat "$TEST_DIR/output.nq")
    [ "$expected" = "$actual" ]
}

@test "mem: load turtle data and output trig" {
    run rpt integrate -e mem "$TEST_DIR/input-triples.ttl" "$TEST_DIR/query-construct-triples.sparql" -o "$TEST_DIR/output.trig"
    [ "$status" -eq 0 ]
    [ -f "$TEST_DIR/output.trig" ]
    
    expected=$(cat "$TEST_DIR/expected-trig.trig")
    actual=$(cat "$TEST_DIR/output.trig")
    [ "$expected" = "$actual" ]
}

teardown() {
    rm -f "$TEST_DIR/output.ttl" "$TEST_DIR/output.nt" "$TEST_DIR/output.nq" "$TEST_DIR/output.trig"
}
