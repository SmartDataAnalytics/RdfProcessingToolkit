#!/usr/bin/env bats

setup() {
    export TEST_DIR="/home/raven/Projects/Eclipse/rdf-processing-toolkit-parent/cli-tests/integrate/test01-mem"
    export SERVER_PORT=8670
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
        pkill -f "rpt integrate.*$SERVER_PORT" 2>/dev/null || true
        sleep 1
    fi
    SERVER_PID=""
}
