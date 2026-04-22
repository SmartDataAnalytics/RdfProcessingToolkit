# Testing RDF Processing Toolkit (`rpt integrate`)

This document describes the test setup for the `rpt integrate` command.

## Test Structure

```
cli-tests/integrate/
├── test01-mem/           # Tests for in-memory engine (Jena mem)
├── test02-tdb2/          # Tests for TDB2 disk-based engine
├── test03-read-only/     # Tests for read-only server mode
└── test04-server/        # Tests for server HTTP endpoints
```

## JUnit Tests

Located in `rdf-processing-toolkit-cli/src/test/java/org/aksw/sparql_integrate/integrate/`

- `TestIntegrateMem.java` - Tests for mem engine (2 tests)
- `TestIntegrateTdb2.java` - Tests for tdb2 engine (2 tests)
- `TestIntegrateQlever.java` - Tests for QLEVER engine (2 tests)

Run with:
```bash
mvn test -pl rdf-processing-toolkit-cli '-Dtest=TestIntegrate*'
```

### Test Coverage
- **mem engine**: Load Turtle data, run CONSTRUCT query, verify output in Turtle and N-Triples formats
- **tdb2 engine**: Load Turtle data to TDB2 database, run CONSTRUCT query, verify output in Turtle and N-Triples formats
- **qlever engine**: Load Turtle data, run CONSTRUCT query, verify output in Turtle and N-Triples formats (uses Docker via testcontainers)

## BATS Tests

BATS (Bash Automated Testing System) tests for CLI verification.

### Prerequisites
- Install BATS: `brew install bats-core` or from source
- Ensure `rpt` is installed and available in PATH
- Docker must be available for qlever tests (uses testcontainers)
- All test paths use relative paths, so tests can run from any directory

### Running Tests

```bash
# Run all integrate tests
bats cli-tests/integrate/

# Run specific test directory
bats cli-tests/integrate/test01-mem/
bats cli-tests/integrate/test02-tdb2/
bats cli-tests/integrate/test05-qlever/
bats cli-tests/integrate/test03-read-only/
bats cli-tests/integrate/test04-server/

# Run specific test file
bats cli-tests/integrate/test01-mem/test-engine-mem.bats

# Run tests from any directory (paths are relative to test file location)
cd /tmp && bats /path/to/cli-tests/integrate/test01-mem/test-engine-mem.bats
```

### Port Configuration
- **read-only tests**: use ports `PORT_OFFSET` (default 8680) and `PORT_OFFSET+1` (default 8681)
- **server tests**: use port 8690 (isolated to avoid conflicts with read-only tests)
- **engine tests**: no ports used (no server)

### Environment Variables
- `PORT_OFFSET`: Starting port for server tests (default: 8680). Use this to avoid port conflicts when running tests in parallel.

### Test Coverage
- **mem engine** (4 tests):
  - Turtle output format
  - N-Triples output format
  - N-Quads output format
  - TriG output format

- **tdb2 engine** (2 tests):
  - Turtle output format
  - N-Triples output format

- **qlever engine** (2 tests):
  - Turtle output format
  - N-Triples output format (uses Docker via testcontainers)

- **read-only server** (2 tests):
  - Server starts and responds to HTTP requests
  - SPARQL UPDATE queries fail via HTTP (403 Forbidden)

- **server mode** (1 test):
  - Server starts and logs are correct

## Test Data

Each test directory contains:
- `input-triples.ttl` - Simple triple data (4 triples)
- `input-triples-small.ttl` - Minimal test data (1 triple)
- `query-construct-triples.sparql` - CONSTRUCT query for triples
- `expected-*.ttl/.nt/.trig/.nq` - Expected output files

## Supported Output Formats

Tested with:
- N-Triples (`.nt`)
- Turtle (`.ttl`)
- N-Quads (`.nq`)
- TriG (`.trig`)
- JSON-LD
- RDF/XML
- N3

## Engine Support

### mem (In-Memory)
- Fast, no disk I/O
- Default engine
- Ideal for CI/CD

### tdb2 (TDB2 Disk-Based)
- Persists data to disk
- Supports larger datasets
- Test verifies database file creation with `--db-keep` flag

### qlever (QLever)
- Remote SPARQL endpoint (Docker container via testcontainers)
- Supports full SPARQL 1.1
- Test verifies Docker container lifecycle

## Server Mode Tests

### Read-Only Mode
- Server starts successfully
- SPARQL SELECT queries work via HTTP
- SPARQL UPDATE queries fail with 403 Forbidden via HTTP
- CLI updates still work (not affected by --read-only flag)

### Server Tests
- Verify server startup
- Check server logs
- Test HTTP endpoints

## Adding New Tests

### JUnit Tests
1. Add test file: `rdf-processing-toolkit-cli/src/test/java/org/aksw/sparql_integrate/integrate/Test*.java`
2. Use `CmdUtils.callCmd()` to execute `rpt integrate`
3. Compare output with expected results using `assertEquals()`
4. Clean up temporary files in `@After` or finally blocks
5. For engines using Docker (qlever), ensure testcontainers is available

### BATS Tests
1. Add test file: `cli-tests/integrate/test0X-*/test-*.bats`
2. Use `rpt` command in test functions
3. Verify output files or HTTP responses with `curl`
4. Cleanup in `teardown()` function
5. For server tests, use unique ports and proper cleanup with `pkill`

## Known Limitations

- Quad data output requires special handling
- Server tests use port-based isolation to run in parallel
- Test data uses simple Turtle format; consider adding more complex cases
