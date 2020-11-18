package org.aksw.sparql_integrate.cli.main;

import java.awt.Desktop;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.json.RdfJsonUtils;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.rx.SparqlScriptProcessor;
import org.aksw.jena_sparql_api.rx.SparqlScriptProcessor.Provenance;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.update.FluentSparqlService;
import org.aksw.named_graph_stream.cli.main.MainCliNamedGraphStream;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.jena.ext.com.google.common.base.Stopwatch;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.ext.com.google.common.collect.Multimaps;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.TransformUnionQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

public class SparqlIntegrateCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateCmdImpls.class);

    // https://stackoverflow.com/questions/35988192/java-nio-most-concise-recursive-directory-delete
    public static void deleteFolder(Path rootPath) throws IOException {
        if (Files.exists(rootPath)) {
            logger.info("Deleting recursively " + rootPath);
            // before you copy and paste the snippet
            // - read the post till the end
            // - read the javadoc to understand what the code will do
            //
            // a) to follow softlinks (removes the linked file too) use
            // Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
            //
            // b) to not follow softlinks (removes only the softlink) use
            // the snippet below
            try (Stream<Path> walk = Files.walk(rootPath)) {
                walk.sorted(Comparator.reverseOrder())
//                    .peek(path -> logger.info("Deleting " + path))
                    .map(Path::toFile)
                    .forEach(File::delete);
    //                .forEach(System.err::println);
            }
        }
    }

    /**
     * Set up a 'DataSource' for RDF.
     *
     * TODO The result should be closable as we may e.g. start a docker container here in the future
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    public static Entry<Dataset, Closeable> configEngine(CmdSparqlIntegrateMain cmd) throws IOException {

        String engine = cmd.engine;

        Entry<Dataset, Closeable> result;
        // TODO Create a registry for engines / should probably go to the conjure project
        if (engine == null || engine.equals("mem")) {
            result = Maps.immutableEntry(DatasetFactory.create(), () -> {});
        } else if (engine.equalsIgnoreCase("tdb2")) {

            boolean createdDbDir = false;
            Path dbPath;
            String pathStr = cmd.dbPath;

            if (Strings.isNullOrEmpty(pathStr)) {
                Path tempDir = Paths.get(cmd.tempPath);
                dbPath = Files.createTempDirectory(tempDir, "sparql-integrate-tdb2-").toAbsolutePath();
                createdDbDir = true;
            } else {
                dbPath = Paths.get(pathStr).toAbsolutePath();
                if (!Files.exists(dbPath)) {
                    Files.createDirectories(dbPath);
                    createdDbDir = true;
                }
            }

            Path finalDbPath = dbPath;
            Closeable deleteAction;
            if (createdDbDir) {
                if (cmd.dbKeep) {
                    logger.info("Created new directory (will be kept after done): " + dbPath);
                    deleteAction = () -> {};
                } else {
                    logger.info("Created new directory (its content will deleted when done): " + dbPath);
                    deleteAction = () -> deleteFolder(finalDbPath);
                }
            } else {
                logger.warn("Folder already existed - delete action disabled: " + dbPath);
                deleteAction = () -> {};
            }

            logger.info("Connecting to TDB2 database in folder " + dbPath);
            Closeable finalDeleteAction = deleteAction;
            result = Maps.immutableEntry(TDB2Factory.connectDataset(finalDbPath.toString()), finalDeleteAction);
        } else {
            throw new RuntimeException("Unknown engine: " + engine);
        }
        return result;
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
            result = OutputMode.TRIPLE;
        } else {
            SparqlStmt last = stmts.get(stmts.size() - 1);
            if (last.isQuery()) {
                Query q = last.getQuery();
                if (q.isJsonType()) {
                    result = OutputMode.JSON;
                }
            }


            int tripleCount = 0;
            int quadCount = 0;
            int bindingCount = 0;

            if (result == null) {
                for (SparqlStmt stmt : stmts) {
                    if (stmt.isQuery()) {
                        Query q = stmt.getQuery();

                        if (q.isConstructType()) {
                            if (q.isConstructQuad()) {
                                ++quadCount;
                            } else {
                                ++tripleCount;
                            }
                        } else if (q.isSelectType()) {
                            ++bindingCount;
                        }
                    }
                }

                if (quadCount != 0) {
                    result = OutputMode.QUAD;
                } else if (tripleCount != 0) {
                    result = OutputMode.TRIPLE;
                } else if (bindingCount != 0) {
                    result = OutputMode.BINDING;
                }
            }


            if(result == null) {
                result = OutputMode.TRIPLE;
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
        int exitCode = 0; // success unless error


        CliUtils.configureGlobalSettings();


        Stopwatch sw = Stopwatch.createStarted();

        PrefixMapping prefixMapping = CliUtils.configPrefixMapping(cmd);

        SparqlScriptProcessor processor = SparqlScriptProcessor.createWithEnvSubstitution(prefixMapping);

        if (cmd.unionDefaultGraph) {
            processor.addPostTransformer(stmt -> SparqlStmtUtils.applyOpTransform(stmt,
                    op -> Transformer.transformSkipService(new TransformUnionQuery(), op)));
        }

        List<String> args = cmd.nonOptionArgs;


        String outFormat = cmd.outFormat;


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
        if (!Strings.isNullOrEmpty(outFilename)) {
            outFile = Paths.get(outFilename).toAbsolutePath();
            if(Files.exists(outFile) && !Files.isWritable(outFile)) {
                throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
            }

            if (outFormat == null) {
                Lang lang = RDFDataMgr.determineLang(outFilename, null, null);
                if (lang != null) {
                    RDFFormat fmt = RDFWriterRegistry.defaultSerialization(lang);
                    outFormat = fmt == null ? null : fmt.toString();
                    logger.info("Inferred output format from " + outFilename + ": " + outFormat);
                }
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
            operationalOut = MainCliNamedGraphStream.openStdout();
            outFile = null;
            tmpFile = null;
        }


        processor.process(args);



        Path splitFolder = cmd.splitFolder == null
                ? null
                : Paths.get(cmd.splitFolder);

        List<Entry<SparqlStmt, Provenance>> workloads = processor.getSparqlStmts();

        // Workloads clustered by their split target filenames
        // If there are no splits then there is one cluster whose key is the empty string
        Multimap<String, Entry<SparqlStmt, Provenance>> clusters;

        if (splitFolder == null) {
            clusters = Multimaps.index(workloads, item -> "");
        } else {
            Files.createDirectories(splitFolder);

            clusters = Multimaps.index(workloads, item -> item.getValue().getSparqlPath());
        }


        Map<String, SPARQLResultExProcessor> clusterToSink = new LinkedHashMap<>();


//
//        if (splitFolder != null) {
//            Multimap<String, Multimaps.index(workloads, item -> item.getValue().getSparqlPath());
//        }
//        List<SparqlStmt> stmts = workloads.stream().map(Entry::getKey).collect(Collectors.toList());

        // Create the union of variables used in select queries
        boolean jqMode = cmd.jqDepth != null;
        int jqDepth = jqMode ? cmd.jqDepth : 3;
        boolean jqFlatMode = cmd.jqFlatMode;

        RDFFormat tripleFormat = RDFFormat.TURTLE_BLOCKS;
        RDFFormat quadFormat = RDFFormat.TRIG_BLOCKS;

        for (Entry<String, Collection<Entry<SparqlStmt, Provenance>>> e : clusters.asMap().entrySet()) {
            List<SparqlStmt> clusterStmts = e.getValue().stream().map(Entry::getKey).collect(Collectors.toList());

            String filename = e.getKey();
            OutputStream effOut;
            if (Strings.isNullOrEmpty(filename)) {
                effOut = operationalOut;
            } else {
                OutputFormatSpec spec = OutputFormatSpec.create(outFormat, tripleFormat, quadFormat, clusterStmts, jqMode);
                String fileExt = spec.getFileExtension();
                fileExt = fileExt == null ? "dat" : fileExt;

                String baseFilename = org.apache.jena.ext.com.google.common.io.Files.getNameWithoutExtension(filename);
                String newFilename = baseFilename + "." + fileExt;

                Path clusterOutFile = splitFolder.resolve(newFilename);
                logger.info("Split: " + filename + " -> " + clusterOutFile);
                effOut = Files.newOutputStream(clusterOutFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
            }

            SPARQLResultExProcessor effectiveHandler = configureProcessor(
                    effOut, System.err,
                    outFormat,
                    clusterStmts,
                    prefixMapping,
                    tripleFormat,
                    quadFormat,
                    jqMode, jqDepth, jqFlatMode,
                    effOut
                    );

            clusterToSink.put(filename, effectiveHandler);
        }



        // Start the engine (wrooom)

        Entry<Dataset, Closeable> datasetAndDelete = configEngine(cmd);
        Dataset dataset = datasetAndDelete.getKey();
        Closeable deleteAction = datasetAndDelete.getValue();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteAction.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));


        // Start all sinks
        for (SPARQLResultExProcessor handler : clusterToSink.values()) {
            handler.start();
        }

        try {
            Callable<RDFConnection> connSupp = () -> RDFConnectionFactory.connect(dataset);


            try (RDFConnection serverConn = (cmd.server ? connSupp.call() : null)) {

                Server server = null;
                if (cmd.server) {
                    SparqlService sparqlService = FluentSparqlService.from(serverConn).create();

    //                Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
    //                        prefixMapping, false);// .getQueryParser();

                    int port = cmd.serverPort;
                    server = FactoryBeanSparqlServer.newInstance()
                            .setSparqlServiceFactory((serviceUri, datasetDescription, httpClient) -> sparqlService)
                            .setSparqlStmtParser(processor.getSparqlParser())
                            .setPort(port).create();

                    server.start();

                    URI browseUri = new URI("http://localhost:" + port + "/sparql");
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(browseUri);
                    } else {
                        logger.info("SPARQL service with in-memory result dataset running at " + browseUri);
                    }

                }

                try(RDFConnection conn = connSupp.call()) {
                    for (Entry<SparqlStmt, Provenance> stmtEntry : workloads) {
                        Provenance prov = stmtEntry.getValue();
                        String clusterId = splitFolder == null ? "" : prov.getSparqlPath();

                        SPARQLResultExProcessor sink = clusterToSink.get(clusterId);

                        try {
                            execStmt(conn, stmtEntry, sink);
                        } catch (Exception e) {
                            logger.error("Error encountered; trying to continue but exit code will be non-zero", e);
                            exitCode = 1;
                        }
                    }

                }

                for (SPARQLResultExProcessor sink : clusterToSink.values()) {
                    sink.finish();
                    sink.flush();
                }


                // Sinks such as SinkQuadOutput may use their own caching / flushing strategy
                // therefore calling flush is mandatory!
                if(outFile != null) {
                    Files.move(tmpFile, outFile, StandardCopyOption.REPLACE_EXISTING);
                }

                logger.info("SPARQL overall execution finished after " + sw.stop());


                if (server != null) {
                    logger.info("Server still running on port " + cmd.serverPort + ". Terminate with CTRL+C");
                    server.join();
                }
            }
        } finally {
            dataset.close();
            deleteAction.close();

            for (SPARQLResultExProcessor sink : clusterToSink.values()) {
                try {
                    sink.close();
                } catch (Exception e) {
                    logger.warn("Failed to close sink", e);
                }
            }

        }

        return exitCode;
    }

    public static void execStmt(
            RDFConnection conn,
            Entry<? extends SparqlStmt, ? extends Provenance> workload,
            SPARQLResultExVisitor<?> resultProcessor) {
//        for (Entry<? extends SparqlStmt, ? extends Provenance> workload : workloads) {
            SparqlStmt stmt = workload.getKey();
            Provenance prov = workload.getValue();
            logger.info("Processing " + prov);
            TxnType txnType = stmt.isQuery() ? TxnType.READ : TxnType.WRITE;

            Txn.exec(conn, txnType, () -> {
                try(SPARQLResultEx sr = SparqlStmtUtils.execAny(conn, stmt)) {
                    resultProcessor.forwardEx(sr);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
//        }
    }


    public static class OutputFormatSpec {
        protected OutputMode outputMode;
        protected RDFFormat outRdfFormat = null;
        protected Lang outLang = null;

        public OutputFormatSpec(OutputMode outputMode, RDFFormat outRdfFormat, Lang outLang) {
            super();
            this.outputMode = outputMode;
            this.outRdfFormat = outRdfFormat;
            this.outLang = outLang;
        }

        public OutputMode getOutputMode() {
            return outputMode;
        }

        public RDFFormat getOutRdfFormat() {
            return outRdfFormat;
        }

        public Lang getOutLang() {
            return outLang;
        }

        public static OutputFormatSpec create(
                String outFormat,
                RDFFormat tripleFormat,
                RDFFormat quadFormat,
                List<SparqlStmt> stmts,
                boolean jqMode
            ) {
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
                    // outRdfFormat = new RDFFormat(outLang);
                    break;
                case TRIPLE:
                    outRdfFormat = tripleFormat;
                    outLang = outRdfFormat.getLang();
                    break;
                case QUAD:
                    outRdfFormat = quadFormat;
                    outLang = outRdfFormat.getLang();
                    break;
                case JSON:
                    // Nothing to do
                    break;
                default:
                    throw new IllegalStateException("Unknown output mode");
                }

            }

            return new OutputFormatSpec(outputMode, outRdfFormat, outLang);
        }

        public String getFileExtension() {
            String result;
            if (outputMode.equals(OutputMode.JSON)) {
                result = "json";
            } else {
                Lang lang;
                if (outRdfFormat != null) {
                    lang = outRdfFormat.getLang();
                } else {
                    lang = outLang;
                }

                if (lang == null) {
                    result = null;
                } else {
                    result = Iterables.getFirst(lang.getFileExtensions(), null);
                }
            }

            return result;
        }
    }



    /**
     *
     * @param outFormat
     * @param stmts
     * @param prefixMapping
     * @param quadFormat Allows to preset a streaming format in case quads were requested
     * @param jqMode
     * @param jqDepth
     * @param jqFlatMode
     * @return
     */
    public static SPARQLResultExProcessor configureProcessor(
            OutputStream out,
            OutputStream err,
            String outFormat,
            List<SparqlStmt> stmts,
            PrefixMapping prefixMapping,
            RDFFormat tripleFormat,
            RDFFormat quadFormat,
            boolean jqMode, int jqDepth, boolean jqFlatMode,
            Closeable closeAction) {

        OutputFormatSpec spec = OutputFormatSpec.create(outFormat, tripleFormat, quadFormat, stmts, jqMode);

        // RDFLanguagesEx.findRdfFormat(cmd.outFormat, probeFormats)
        List<Var> selectVars = SparqlStmtUtils.getUnionProjectVars(stmts);

        SPARQLResultExProcessorImpl coreProcessor = SPARQLResultExProcessorImpl.configureForOutputMode(
                spec.getOutputMode(),
                out,
                err,
                prefixMapping,
                spec.getOutRdfFormat(),
                spec.getOutLang(),
                selectVars,
                closeAction);


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
