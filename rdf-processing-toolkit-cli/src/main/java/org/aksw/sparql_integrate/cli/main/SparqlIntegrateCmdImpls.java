package org.aksw.sparql_integrate.cli.main;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.json.RdfJsonUtils;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.update.FluentSparqlService;
import org.aksw.named_graph_stream.cli.main.MainCliNamedGraphStream;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_integrate.cli.SparqlScriptProcessor;
import org.aksw.sparql_integrate.cli.SparqlScriptProcessor.Provenance;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.jena.ext.com.google.common.base.Stopwatch;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.TransformUnionQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.Var;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

public class SparqlIntegrateCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateCmdImpls.class);

    public static Callable<RDFConnection> configConnection(CmdSparqlIntegrateMain cmd) {
        Dataset dataset = DatasetFactory.create();
        Callable<RDFConnection> result = () -> RDFConnectionFactory.connect(dataset);
        return result;
//    	Dataset ds = DatasetFactory.create;
//    	RDFConnection conn =
//
//        try(RDFConnection actualConn = RDFConnectionFactoryEx.wrapWithContext(
//                RDFConnectionFactoryEx.wrapWithQueryParser(RDFConnectionFactory.connect(dataset),
//                str -> {
//                    SparqlStmt stmt = sparqlParser.apply(str);
//                    SparqlStmt r = SparqlStmtUtils.applyNodeTransform(stmt, x -> NodeUtils.substWithLookup(x, System::getenv));
//                    return r;
//                }))) {

    }



    /**
     * If the last query is a json query then the mode is json.
     * If there is a construct query the mode is quads.
     * If there is no construct query but a select one, the mode is bindings.
     *
     */
    public static OutputMode detectOutputMode(List<SparqlStmt> stmts) {
        OutputMode result = null;
        if (stmts.isEmpty()) {
            result = OutputMode.QUAD;
        } else {
            SparqlStmt last = stmts.get(stmts.size() - 1);
            if (last.isQuery()) {
                Query q = last.getQuery();
                if (q.isJsonType()) {
                    result = OutputMode.JSON;
                }
            }


            int quadCount = 0;
            int bindingCount = 0;
            if (result == null) {
                for (SparqlStmt stmt : stmts) {
                    if (stmt.isQuery()) {
                        Query q = stmt.getQuery();

                        if (q.isConstructType()) {
                            ++quadCount;
                        } else if (q.isSelectType()) {
                            ++bindingCount;
                        }
                    }
                }

                if (quadCount != 0) {
                    result = OutputMode.QUAD;
                } else if (bindingCount != 0) {
                    result = OutputMode.BINDING;
                }
            }


            if(result == null) {
                result = OutputMode.QUAD;
            }
        }

        return result;
    }


    public static OutputMode determineOutputMode(Lang lang) {
        OutputMode result;
        if (RDFLanguages.isTriples(lang) || RDFLanguages.isQuads(lang)) {
            result = OutputMode.QUAD;
        } else if (ResultSetWriterRegistry.isRegistered(lang)) {
            result = OutputMode.BINDING;
        } else {
            //result = OutputMode.UNKOWN;
            result = null;
        }

        return result;
    }



    public static int sparqlIntegrate(CmdSparqlIntegrateMain cmd) throws Exception {
        CliUtils.configureGlobalSettings();

        Stopwatch sw = Stopwatch.createStarted();

        PrefixMapping prefixMapping = CliUtils.configPrefixMapping(cmd);

        SparqlScriptProcessor processor = SparqlScriptProcessor.create(prefixMapping);
        processor.addPostTransformer(stmt -> SparqlStmtUtils.applyOpTransform(stmt,
                op -> Transformer.transformSkipService(new TransformUnionQuery(), op)));

        List<String> args = cmd.nonOptionArgs;

        // If an in/out file is given prepend it to the arguments
        Path outFile = null;
        String outFilename = null;
        Path tmpFile;
        if (cmd.outputSpec != null) {
            OutputSpec outputSpec = cmd.outputSpec;
            if (outputSpec.inOutFile != null) {
                outFilename = outputSpec.inOutFile;
                args.listIterator().add(outFilename);
            } else if (outputSpec.outFile != null) {
                outFilename = outputSpec.outFile;
            }
        }

        OutputStream operationalOut;
        if(!Strings.isNullOrEmpty(outFilename)) {
            outFile = Paths.get(outFilename).toAbsolutePath();
            if(Files.exists(outFile) && !Files.isWritable(outFile)) {
                throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
            }

            Path parent = outFile.getParent();
            String tmpName = "." + outFile.getFileName().toString() + ".tmp";
            tmpFile = parent.resolve(tmpName);

            operationalOut = Files.newOutputStream(tmpFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // Register a shutdown hook to delete the temporary file
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    operationalOut.close();
                    Files.deleteIfExists(tmpFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));

        } else {
            operationalOut = MainCliNamedGraphStream.out;
            outFile = null;
            tmpFile = null;
        }

        processor.process(args);

        List<Entry<SparqlStmt, Provenance>> workloads = processor.getSparqlStmts();

        List<SparqlStmt> stmts = workloads.stream().map(Entry::getKey).collect(Collectors.toList());

        // Create the union of variables used in select queries

        boolean jqMode = cmd.jqDepth != null;
        int jqDepth = jqMode ? cmd.jqDepth : 3;
        boolean jqFlatMode = cmd.jqFlatMode;

        String outFormat = cmd.outFormat;


        SPARQLResultExProcessor effectiveHandler = configureProcessor(
                outFormat,
                stmts,
                prefixMapping,
                jqMode, jqDepth, jqFlatMode);


        effectiveHandler.start();

        Callable<RDFConnection> connSupp = configConnection(cmd);


        try (RDFConnection serverConn = (cmd.server ? connSupp.call() : null)) {

            Server server = null;
            if (cmd.server) {
                SparqlService sparqlService = FluentSparqlService.from(serverConn).create();

//                Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
//                        prefixMapping, false);// .getQueryParser();

                int port = 7532;
                server = FactoryBeanSparqlServer.newInstance()
                        .setSparqlServiceFactory((serviceUri, datasetDescription, httpClient) -> sparqlService)
                        .setSparqlStmtParser(processor.getSparqlParser())
                        .setPort(port).create();

                server.start();

                URI browseUri = new URI("http://localhost:" + port + "/sparql");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(browseUri);
                } else {
                    System.err.println("SPARQL service with in-memory result dataset running at " + browseUri);
                }

            }

            try(RDFConnection conn = connSupp.call()) {
                execStmts(conn, workloads, effectiveHandler);
            }

            effectiveHandler.finish();

            operationalOut.flush();

            if(outFile != null) {
                operationalOut.close();
                Files.move(tmpFile, outFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("SPARQL overall execution finished after " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");


            if (server != null) {
                server.join();
            }
        }

        return 0;
    }

    public static void execStmts(
            RDFConnection conn,
            Collection<? extends Entry<? extends SparqlStmt, ? extends Provenance>> workloads,
            SPARQLResultExVisitor<?> resultProcessor) {
        for (Entry<? extends SparqlStmt, ? extends Provenance> workload : workloads) {
            SparqlStmt stmt = workload.getKey();
            Provenance prov = workload.getValue();
            logger.info("Processing " + prov);
            try(SPARQLResultEx sr = SparqlStmtUtils.execAny(conn, stmt)) {
                resultProcessor.forwardEx(sr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static SPARQLResultExProcessor configureProcessor(
            String outFormat,
            List<SparqlStmt> stmts,
            PrefixMapping prefixMapping,
            boolean jqMode, int jqDepth, boolean jqFlatMode) {

        OutputMode outputMode;
        RDFFormat outRdfFormat = null;
        Lang outLang = null;

        if (outFormat != null) {
            if ("json".equalsIgnoreCase(outFormat)) {
                outputMode = OutputMode.JSON;
            } else {

                try {
                    outRdfFormat = RDFLanguagesEx.findRdfFormat(outFormat);
                    outLang = outRdfFormat.getLang();
                } catch (Exception e) {
                    outLang = RDFLanguagesEx.findLang(outFormat);
                }

                outputMode = determineOutputMode(outLang);
            }
        } else {
            outputMode = jqMode ? OutputMode.JSON : detectOutputMode(stmts);

            switch (outputMode) {
            case BINDING:
                outLang = ResultSetLang.SPARQLResultSetJSON;
                break;
            case QUAD:
                outRdfFormat = RDFFormat.TRIG_BLOCKS;
                outLang = outRdfFormat.getLang();
                break;
            case JSON:
                // Nothing to do
                break;
            default:
                throw new IllegalStateException("Unkwon outputMode");
            }

        }


        // RDFLanguagesEx.findRdfFormat(cmd.outFormat, probeFormats)
        List<Var> selectVars = SparqlStmtUtils.getUnionProjectVars(stmts);

        SPARQLResultExProcessorImpl coreProcessor = SPARQLResultExProcessorImpl.configureForOutputMode(
                outputMode,
                MainCliNamedGraphStream.out,
                System.err,
                prefixMapping,
                outRdfFormat,
                outLang,
                selectVars);


        // TODO The design with SPARQLResultExProcessorForwarding seems a bit overly complex
        // Perhaps allow setting up the jq stuff on SPARQLResultExProcessorImpl directly?
        SPARQLResultExProcessor effectiveProcessor;
        if (jqMode) {
            effectiveProcessor = new SPARQLResultExProcessorForwarding<SPARQLResultExProcessorImpl>(coreProcessor) {
                @Override
                public Void onResultSet(ResultSet it) {
                    while (it.hasNext()) {
                        QuerySolution qs = it.next();
                        JsonElement json = RdfJsonUtils.toJson(qs, jqDepth, jqFlatMode);
                        coreProcessor.getJsonSink().send(json);
                    }
                    return null;
                }
            };
        } else {
            effectiveProcessor = coreProcessor;
        }
        return effectiveProcessor;
    }
}
