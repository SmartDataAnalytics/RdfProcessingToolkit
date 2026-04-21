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

public class TestIntegrateQlever {
    static { CliUtils.configureGlobalSettings(); }

    private static final String TEST_DATA_DIR = "integrate/test05-qlever/";

    @Test
    public void testQleverEngineWithTurtleInput() throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources/");
        String inputFile = baseDir.resolve(TEST_DATA_DIR + "input-triples.ttl").toString();
        String queryFile = baseDir.resolve(TEST_DATA_DIR + "query-construct-triples.sparql").toString();
        String expectedPath = baseDir.resolve(TEST_DATA_DIR + "expected-turtle.ttl").toString();

        Path tempOut = Files.createTempFile("test-qlever-", ".ttl");
        try {
            String[] args = { "integrate", "-e", "qlever", inputFile, queryFile, "-o", tempOut.toString() };
            CmdUtils.callCmd(CmdRptMain.class, args);

            String expected = new String(Files.readAllBytes(Paths.get(expectedPath)), StandardCharsets.UTF_8);
            String actual = new String(Files.readAllBytes(tempOut), StandardCharsets.UTF_8);

            assertEquals("QLEVER output should match expected", expected.trim(), actual.trim());
        } finally {
            Files.deleteIfExists(tempOut);
        }
    }

    @Test
    public void testQleverEngineWithNtriplesOutput() throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources/");
        String inputFile = baseDir.resolve(TEST_DATA_DIR + "input-triples.ttl").toString();
        String queryFile = baseDir.resolve(TEST_DATA_DIR + "query-construct-triples.sparql").toString();
        String expectedPath = baseDir.resolve(TEST_DATA_DIR + "expected-ntriples.nt").toString();

        Path tempOut = Files.createTempFile("test-qlever-", ".nt");
        try {
            String[] args = { "integrate", "-e", "qlever", inputFile, queryFile, "-o", tempOut.toString() };
            CmdUtils.callCmd(CmdRptMain.class, args);

            String expected = new String(Files.readAllBytes(Paths.get(expectedPath)), StandardCharsets.UTF_8);
            String actual = new String(Files.readAllBytes(tempOut), StandardCharsets.UTF_8);

            assertEquals("QLEVER N-Triples output should match expected", expected.trim(), actual.trim());
        } finally {
            Files.deleteIfExists(tempOut);
        }
    }
}
