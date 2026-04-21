package org.aksw.sparql_integrate.integrate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.aksw.commons.picocli.CmdUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestIntegrateTdb2 {
    static { CliUtils.configureGlobalSettings(); }

    private static final String TEST_DATA_DIR = "integrate/test02-tdb2/";

    @Test
    public void testTdb2EngineWithTurtleInput() throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources/");
        String inputFile = baseDir.resolve(TEST_DATA_DIR + "input-triples.ttl").toString();
        String queryFile = baseDir.resolve(TEST_DATA_DIR + "query-construct-triples.sparql").toString();
        String expectedPath = baseDir.resolve(TEST_DATA_DIR + "expected-turtle.ttl").toString();

        Path tempDb = Files.createTempDirectory("tdb2-test-");
        Path tempOut = Files.createTempFile("test-tdb2-", ".ttl");
        
        try {
            String[] args = { 
                "integrate", 
                "-e", "tdb2", 
                "--loc", tempDb.toString(),
                inputFile, 
                queryFile, 
                "-o", tempOut.toString(),
                "--db-keep"
            };
            CmdUtils.callCmd(CmdRptMain.class, args);

            String expected = new String(Files.readAllBytes(Paths.get(expectedPath)), StandardCharsets.UTF_8);
            String actual = new String(Files.readAllBytes(tempOut), StandardCharsets.UTF_8);

            assertEquals("TDB2 output should match expected", expected.trim(), actual.trim());
        } finally {
            Files.deleteIfExists(tempOut);
            try {
                Path dbPath = Paths.get(tempDb.toString());
                deleteRecursively(dbPath);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    public void testTdb2EngineWithNtriplesOutput() throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources/");
        String inputFile = baseDir.resolve(TEST_DATA_DIR + "input-triples.ttl").toString();
        String queryFile = baseDir.resolve(TEST_DATA_DIR + "query-construct-triples.sparql").toString();
        String expectedPath = baseDir.resolve(TEST_DATA_DIR + "expected-ntriples.nt").toString();

        Path tempDb = Files.createTempDirectory("tdb2-test-");
        Path tempOut = Files.createTempFile("test-tdb2-", ".nt");
        
        try {
            String[] args = { 
                "integrate", 
                "-e", "tdb2", 
                "--loc", tempDb.toString(),
                inputFile, 
                queryFile, 
                "-o", tempOut.toString(),
                "--db-keep"
            };
            CmdUtils.callCmd(CmdRptMain.class, args);

            String expected = new String(Files.readAllBytes(Paths.get(expectedPath)), StandardCharsets.UTF_8);
            String actual = new String(Files.readAllBytes(tempOut), StandardCharsets.UTF_8);

            assertEquals("TDB2 N-Triples output should match expected", expected.trim(), actual.trim());
        } finally {
            Files.deleteIfExists(tempOut);
            try {
                Path dbPath = Paths.get(tempDb.toString());
                deleteRecursively(dbPath);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(p -> {
                try {
                    deleteRecursively(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.deleteIfExists(path);
    }
}
