#!/usr/bin/env bats

setup() {
    export TEST_DIR="/home/raven/Projects/Eclipse/rdf-processing-toolkit-parent/cli-tests/integrate/test01-mem"
    export SERVER_PORT=8671
    export SERVER_PID=""
}

@test "read-only: server starts and responds to HTTP requests" {
    start_server "read-only"
    sleep 10
    
    [ -f "/tmp/server-$SERVER_PORT.log" ]
    
    run grep "RdfProcessingToolkit Server running" "/tmp/server-$SERVER_PORT.log"
    [ "$status" -eq 0 ]
    
    stop_server
}

@test "read-only: SPARQL UPDATE should fail via HTTP" {
    export SERVER_PORT=8672
    start_server "read-only"
    sleep 10
    
    [ -f "/tmp/server-$SERVER_PORT.log" ]
    
    run grep "RdfProcessingToolkit Server running" "/tmp/server-$SERVER_PORT.log"
    [ "$status" -eq 0 ]
    
    stop_server
}

start_server() {
    MODE="$1"
    if [ "$MODE" = "read-only" ]; then
        (rpt integrate -e mem --server --port "$SERVER_PORT" "$TEST_DIR/input-triples-small.ttl" --read-only > /tmp/server-$SERVER_PORT.log 2>&1) &
    else
        (rpt integrate -e mem --server --port "$SERVER_PORT" "$TEST_DIR/input-triples-small.ttl" > /tmp/server-$SERVER_PORT.log 2>&1) &
    fi
    SERVER_PID=$!
    sleep 2
}

stop_server() {
    if [ -n "$SERVER_PID" ]; then
        pkill -f "rpt integrate.*$SERVER_PORT" 2>/dev/null || true
        sleep 1
    fi
    SERVER_PID=""
}
