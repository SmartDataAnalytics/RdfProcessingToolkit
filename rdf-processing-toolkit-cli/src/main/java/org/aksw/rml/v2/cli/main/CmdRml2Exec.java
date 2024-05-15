package org.aksw.rml.v2.cli.main;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.aksw.commons.io.util.FileUtils;
import org.aksw.jenax.arq.picocli.CmdMixinRdfOutput;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdCommonBase;
import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;
import org.aksw.rml.jena.impl.RmlToSparqlRewriteBuilder;
import org.aksw.rml.jena.plugin.ReferenceFormulationRegistry;
import org.aksw.rml.jena.ref.impl.ReferenceFormulationJsonStrViaService;
import org.aksw.rml.v2.common.vocab.RmlIoTerms;
import org.aksw.rml.v2.jena.domain.api.TriplesMapRml2;
import org.aksw.rmltk.rml.processor.RmlTestCase;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFOps;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sys.JenaSystem;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rml2exec",
versionProvider = VersionProviderRdfProcessingToolkit.class,
description = "Run RML2 mappings")
public class CmdRml2Exec
    extends CmdCommonBase
    implements Callable<Integer>
{
    static { JenaSystem.init(); }

    @Option(names = { "--mapping-directory" }, description="Directory against which to resolve relative paths")
    public Path mappingDirectory;

    @Parameters(arity = "0..*", description = "File names with RML2 Mappings")
    public List<Path> rml2MappingFiles = new ArrayList<>();

    @Mixin
    public CmdMixinRdfOutput rdfOutputConfig = new CmdMixinRdfOutput();

    @Override
    public Integer call() throws Exception {

        ReferenceFormulationRegistry rfRegistry = new ReferenceFormulationRegistry();
        ReferenceFormulationRegistry.registryDefaults(rfRegistry);

        // Override registration for JSON to *not* use natural mappings
        rfRegistry.put(RmlIoTerms.JSONPath, new ReferenceFormulationJsonStrViaService());


        RmlToSparqlRewriteBuilder builder = new RmlToSparqlRewriteBuilder()
                .setRegistry(rfRegistry)
                // .setCache(cache)
                // .addFnmlFiles(fnmlFiles)
                .addRmlPaths(TriplesMapRml2.class, rml2MappingFiles)
                // .addRmlModel(TriplesMapRml2.class, rmlMapping)
                .setDenormalize(false)
                .setDistinct(true)
                // .setMerge(true)
                ;

        List<Entry<Query, String>> labeledQueries = builder.generate();

        Dataset dataset = RmlTestCase.execute(labeledQueries, mappingDirectory, null);

        try (PrintStream out = new PrintStream(FileUtils.newOutputStream(rdfOutputConfig), false, StandardCharsets.UTF_8)) {
            StreamRDF writer = StreamRDFWriter.getWriterStream(out, RDFFormat.NQUADS);
            writer.start();
            StreamRDFOps.sendDatasetToStream(dataset.asDatasetGraph(), writer);
            writer.finish();
            out.flush();
        }

        return 0;
    }
}
