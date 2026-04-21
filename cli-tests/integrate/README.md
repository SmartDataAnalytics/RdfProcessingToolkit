# Integration Tests for `rpt integrate`

This directory contains CLI tests for the RDF Processing Toolkit's `rpt integrate` command.

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

## Test Data

Each test directory contains:
- `input-triples.ttl` - Simple triple data
- `input-triples-small.ttl` - Minimal test data (1 triple)
- `input-quads.trig` - Data with named graphs
- `query-construct-triples.sparql` - CONSTRUCT query for triples
- `query-construct-quads.sparql` - CONSTRUCT query for quads
- `expected-*` - Expected output files

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
- Test verifies database file creation

## Test Coverage

### JUnit Tests (4 tests)
- `TestIntegrateMem`: mem engine with Turtle and N-Triples output
- `TestIntegrateTdb2`: tdb2 engine with Turtle and N-Triples output

### BATS Tests (9 tests)
- **mem engine**: Turtle, N-Triples, N-Quads, TriG output formats
- **tdb2 engine**: Turtle, N-Triples output formats
- **read-only server**: HTTP endpoint access, UPDATE query rejection
- **server mode**: Server startup verification

## Adding New Tests

1. Create test data files in appropriate directory
2. Write JUnit test in `TestIntegrate*.java`
3. Write BATS test in `*.bats`
4. Run tests to verify

## Adding New Tests

1. Create test data files in appropriate directory
2. Write JUnit test in `TestIntegrate*.java`
3. Write BATS test in `*.bats`
4. Run tests to verify
