#!/usr/bin/env bash

# Test runner for RDF Processing Toolkit integrate tests
# Runs both JUnit and BATS tests

set -e

echo "=== Running JUnit Tests ==="
mvn test -pl rdf-processing-toolkit-cli '-Dtest=TestIntegrate*'

echo ""
echo "=== Running BATS Tests ==="
bats cli-tests/integrate/test01-mem/test-engine-mem.bats
bats cli-tests/integrate/test02-tdb2/test-engine-tdb2.bats
bats cli-tests/integrate/test03-read-only/test-read-only.bats
bats cli-tests/integrate/test04-server/test-server.bats

echo ""
echo "=== All Tests Passed ==="
