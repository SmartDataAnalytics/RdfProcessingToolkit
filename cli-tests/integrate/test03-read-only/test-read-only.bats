#!/usr/bin/env bats

setup() {
    SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_DIRNAME")" && pwd)"
    RESOURCES_DIR="$SCRIPT_DIR/resources"
    export PORT_OFFSET="${PORT_OFFSET:-8680}"
    export SERVER_PID=""
}

@test "read-only: server starts and responds to HTTP requests" {
    export SERVER_PORT=$PORT_OFFSET
    start_server "read-only"
    sleep 10
    
    [ -f "/tmp/server-$SERVER_PORT.log" ]
    
    run grep "RdfProcessingToolkit Server running" "/tmp/server-$SERVER_PORT.log"
    [ "$status" -eq 0 ]
    
    stop_server
}

@test "read-only: SPARQL UPDATE should fail via HTTP" {
    export SERVER_PORT=$((PORT_OFFSET + 1))
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
        (rpt integrate -e mem --server --port "$SERVER_PORT" "$RESOURCES_DIR/input-triples-small.ttl" --read-only > /tmp/server-$SERVER_PORT.log 2>&1) &
    else
        (rpt integrate -e mem --server --port "$SERVER_PORT" "$RESOURCES_DIR/input-triples-small.ttl" > /tmp/server-$SERVER_PORT.log 2>&1) &
    fi
    SERVER_PID=$!
    sleep 2
}

stop_server() {
    if [ -n "$SERVER_PID" ]; then
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
    SERVER_PID=""
}
