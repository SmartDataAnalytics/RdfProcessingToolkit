package org.aksw.sparql_integrate.integrate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.aksw.commons.picocli.CmdUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestIntegrateMem {
    static { CliUtils.configureGlobalSettings(); }

    private static final String TEST_DATA_DIR = "integrate/test01-mem/";

    @Test
    public void testMemEngineWithTurtleInput() throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources/");
        String inputFile = baseDir.resolve(TEST_DATA_DIR + "input-triples.ttl").toString();
        String queryFile = baseDir.resolve(TEST_DATA_DIR + "query-construct-triples.sparql").toString();
        String expectedPath = baseDir.resolve(TEST_DATA_DIR + "expected-turtle.ttl").toString();

        Path tempOut = Files.createTempFile("test-mem-", ".ttl");
        try {
            String[] args = { "integrate", "-e", "mem", inputFile, queryFile, "-o", tempOut.toString() };
            CmdUtils.callCmd(CmdRptMain.class, args);

            String expected = new String(Files.readAllBytes(Paths.get(expectedPath)), StandardCharsets.UTF_8);
            String actual = new String(Files.readAllBytes(tempOut), StandardCharsets.UTF_8);

            assertEquals("Output should match expected", normalizeTurtle(expected), normalizeTurtle(actual));
        } finally {
            Files.deleteIfExists(tempOut);
        }
    }

    @Test
    public void testMemEngineWithNtriplesOutput() throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir")).resolve("src/test/resources/");
        String inputFile = baseDir.resolve(TEST_DATA_DIR + "input-triples.ttl").toString();
        String queryFile = baseDir.resolve(TEST_DATA_DIR + "query-construct-triples.sparql").toString();
        String expectedPath = baseDir.resolve(TEST_DATA_DIR + "expected-ntriples.nt").toString();

        Path tempOut = Files.createTempFile("test-mem-", ".nt");
        try {
            String[] args = { "integrate", "-e", "mem", inputFile, queryFile, "-o", tempOut.toString() };
            CmdUtils.callCmd(CmdRptMain.class, args);

            String expected = new String(Files.readAllBytes(Paths.get(expectedPath)), StandardCharsets.UTF_8);
            String actual = new String(Files.readAllBytes(tempOut), StandardCharsets.UTF_8);

            assertEquals("N-Triples output should match expected", expected.trim(), actual.trim());
        } finally {
            Files.deleteIfExists(tempOut);
        }
    }

    private String normalizeTurtle(String turtle) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new java.io.StringReader(turtle), null, "TURTLE");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, model, RDFFormat.TURTLE_BLOCKS);
        return out.toString(StandardCharsets.UTF_8);
    }
}
