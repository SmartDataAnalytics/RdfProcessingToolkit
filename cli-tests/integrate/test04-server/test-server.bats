#!/usr/bin/env bats

# Port configuration - use unique port range to avoid conflicts
# PORT_OFFSET can be set via environment variable, defaults to 8680
# Server test uses port 8690 to avoid conflicts with other server tests

setup() {
    export TEST_DIR="$BATS_TEST_DIRNAME/../test01-mem"
    export SERVER_PORT=8690
    export SERVER_PID=""
}

@test "server: server starts and logs are correct" {
    start_server
    sleep 10
    
    [ -f "/tmp/server-$SERVER_PORT.log" ]
    
    run grep "RdfProcessingToolkit Server running" "/tmp/server-$SERVER_PORT.log"
    [ "$status" -eq 0 ]
    
    stop_server
}

start_server() {
    (rpt integrate -e mem --server --port "$SERVER_PORT" "$TEST_DIR/input-triples-small.ttl" > /tmp/server-$SERVER_PORT.log 2>&1) &
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
