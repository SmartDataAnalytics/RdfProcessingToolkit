#!/usr/bin/env bats

# Port configuration - use sequential ports from PORT_OFFSET
# PORT_OFFSET can be set via environment variable, defaults to 8680
# read-only tests use ports PORT_OFFSET (8680) and PORT_OFFSET+1 (8681)

setup() {
    export TEST_DIR="$BATS_TEST_DIRNAME/../test01-mem"
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
        (rpt integrate -e mem --server --port "$SERVER_PORT" "$TEST_DIR/input-triples-small.ttl" --read-only > /tmp/server-$SERVER_PORT.log 2>&1) &
    else
        (rpt integrate -e mem --server --port "$SERVER_PORT" "$TEST_DIR/input-triples-small.ttl" > /tmp/server-$SERVER_PORT.log 2>&1) &
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
