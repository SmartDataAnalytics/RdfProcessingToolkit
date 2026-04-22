# Integration Tests for `rpt integrate`

This directory contains CLI tests for the RDF Processing Toolkit's `rpt integrate` command.

## Test Structure

```
cli-tests/integrate/
├── resources/               # Centralized test data (shared across all tests)
│   ├── input-triples.ttl
│   ├── input-triples-small.ttl
│   ├── query-construct-triples.sparql
│   ├── expected-turtle.ttl
│   ├── expected-ntriples.nt
│   ├── expected-nquads.nq
│   └── expected-trig.trig
├── test01-mem/              # Tests for in-memory engine (Jena mem)
├── test02-tdb2/             # Tests for TDB2 disk-based engine
├── test03-read-only/        # Tests for read-only server mode
├── test04-server/           # Tests for server HTTP endpoints
└── test05-qlever/           # Tests for QLever engine
```

### Test Directories

- **test01-mem/** - 4 tests: turtle, nt, nq, trig output formats
- **test02-tdb2/** - 2 tests: turtle, nt output formats
- **test03-read-only/** - 2 tests: server start, UPDATE rejection
- **test04-server/** - 1 test: server startup verification
- **test05-qlever/** - 2 tests: turtle, nt output formats (no quads/trig support)

## JUnit Tests

Located in `rdf-processing-toolkit-cli/src/test/java/org/aksw/sparql_integrate/integrate/`

- `TestIntegrateMem.java` - Tests for mem engine
- `TestIntegrateTdb2.java` - Tests for tdb2 engine

Run with:
```bash
mvn test -pl rdf-processing-toolkit-cli -Dtest=TestIntegrate*
```

## BATS Tests

BATS (Bash Automated Testing System) tests for CLI verification.

### Prerequisites
- Install BATS: `brew install bats-core` or from source
- Ensure `rpt` is installed and available in PATH

### Running Tests

```bash
# Run all integrate tests
bats cli-tests/integrate/

# Run specific test directory
bats cli-tests/integrate/test01-mem/
bats cli-tests/integrate/test02-tdb2/

# Run specific test file
bats cli-tests/integrate/test01-mem/test-engine-mem.bats
```

### Test Data Location

All test data is centralized in the `resources/` directory. Tests reference input files and expected outputs via relative paths to `resources/`. All output files are written to temporary directories (created per-test) and cleaned up after test completion.

**Note:** The QLever engine doesn't support CONSTRUCT quads, so test05-qlever only tests turtle and ntriples output formats.

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
- Supports all output formats (turtle, nt, nq, trig)

### tdb2 (TDB2 Disk-Based)
- Persists data to disk
- Supports larger datasets
- Test verifies database file creation
- Supports turtle and nt output formats

### qlever (QLever Engine)
- SPARQL 1.1 compliant query engine
- Supports turtle and nt output formats
- Does NOT support CONSTRUCT quads (no nq/trig)

## Test Coverage

### JUnit Tests (4 tests)
- `TestIntegrateMem`: mem engine with Turtle and N-Triples output
- `TestIntegrateTdb2`: tdb2 engine with Turtle and N-Triples output

### BATS Tests (11 tests)
- **mem engine**: Turtle, N-Triples, N-Quads, TriG output formats
- **tdb2 engine**: Turtle, N-Triples output formats
- **read-only server**: HTTP endpoint access, UPDATE query rejection
- **server mode**: Server startup verification
- **qlever engine**: Turtle, N-Triples output formats (no quads)

## Adding New Tests

1. Add test data files to `resources/` directory
2. Write BATS test in appropriate `test-*/` directory
3. Reference files via `$RESOURCES_DIR` (see existing tests for examples)
4. Write all outputs to `$TEMP_DIR` (created in `setup()`)
5. Clean up temp directory in `teardown()`
6. Run tests to verify

## Adding New Engine Tests

When adding a new engine test directory:

1. Create new directory: `testXX-engine/`
2. Copy relevant test data from `resources/` (if needed for engine-specific data)
3. Create `.bats` file with appropriate tests
4. Follow these conventions:
   - Use `SCRIPT_DIR` and `RESOURCES_DIR` for paths
   - Use `TEMP_DIR=$(mktemp -d)` for outputs
   - Clean up in `teardown()`
   - Skip unsupported formats (e.g., qlever doesn't support nq/trig)
5. Update this README.md to document the new test directory
