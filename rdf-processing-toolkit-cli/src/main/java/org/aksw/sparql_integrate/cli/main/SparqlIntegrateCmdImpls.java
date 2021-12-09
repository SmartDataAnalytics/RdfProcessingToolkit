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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.aksw.commons.io.util.StdIo;
import org.aksw.jena_sparql_api.algebra.transform.TransformCollectOps;
import org.aksw.jena_sparql_api.algebra.visitor.OpVisitorTriplesQuads;
import org.aksw.jena_sparql_api.rx.io.resultset.OutputFormatSpec;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExProcessor;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExProcessorBuilder;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExVisitor;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor.Provenance;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.update.FluentSparqlService;
import org.aksw.jenax.arq.connection.core.RDFConnectionUtils;
import org.aksw.jenax.arq.datasource.RdfDataSourceFactory;
import org.aksw.jenax.arq.datasource.RdfDataSourceFactoryRegistry;
import org.aksw.jenax.arq.datasource.RdfDataSourceFactorySansa;
import org.aksw.jenax.arq.datasource.RdfDataSourceSpecBasicFromMap;
import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.aksw.jenax.connectionless.SparqlService;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.resultset.SPARQLResultEx;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.jena.JenaRuntime;
import org.apache.jena.ext.com.google.common.base.Stopwatch;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.ext.com.google.common.collect.Multimaps;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformUnionQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.engine.main.StageBuilder;
import org.apache.jena.sparql.mgt.Explain.InfoLevel;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.MappingRegistry;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.system.Txn;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlIntegrateCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateCmdImpls.class);

    public static RdfDataSource setupRdfDataSource(CmdSparqlIntegrateMain cmd) throws Exception {

        String sourceType = Optional.ofNullable(cmd.engine).orElse("mem");

        RdfDataSourceFactory factory = RdfDataSourceFactoryRegistry.get().get(sourceType);
        if (factory == null) {
            throw new RuntimeException("No RdfDataSourceFactory registered under name " + sourceType);
        }

        RdfDataSourceSpecBasicFromMap spec = RdfDataSourceSpecBasicFromMap.create();
        spec.setTempDir(cmd.tempPath);
        spec.setAutoDeleteIfCreated(!cmd.dbKeep);
        spec.setLocation(cmd.dbPath);
        spec.setLocationContext(cmd.dbFs);


        RdfDataSource result = factory.create(spec.getMap());
        return result;
    }



    public static RDFConnection wrapWithAutoDisableReorder(RDFConnection conn) {
        return RDFConnectionUtils.wrapWithTransform(conn, null, qe -> {
            // We require the connection already provide an appropriate OpExecutor instance
            // that can handle custom service executors!
            // See: DatasetBasedSparqlEngine.newConnection()

            Query query = qe.getQuery();
            if (query == null) {
                logger.warn("Could not obtain query from query execution.");
            }

            boolean disableOrder = shouldDisableReorder(query);
            logger.info("Triple ordering disabled? " + disableOrder);

            if (disableOrder) {
                StageBuilder.setGenerator(qe.getContext(), StageBuilder.executeInline);
            }

            return qe;
        });
    }

    /**
     * Triple pattern reordering can give significant performance boosts on SPARQL queries
     * but when SERVICE clauses and/or user defined property functions are in use it can
     * lead to unexpected results.
     *
     * This method decides whether to disable reordering
     *
     */
    public static boolean shouldDisableReorder(Query query) {
        Op op = Algebra.toQuadForm(Algebra.compile(query));
        Set<Op> ops = TransformCollectOps.collect(op, false);

        boolean containsService = ops.stream().anyMatch(x -> x instanceof OpService);

        // udpf = user defined property function
        Set<String> usedUdpfs = ops.stream()
            .flatMap(OpVisitorTriplesQuads::streamQuads)
            .map(quad -> quad.getPredicate())
            .filter(Node::isURI)
            .map(Node::getURI)
            .filter(uri -> PropertyFunctionRegistry.get().get(uri) != null)
            .collect(Collectors.toSet());

        boolean usesUdpf = !usedUdpfs.isEmpty();

        boolean result = containsService || usesUdpf;
        return result;
    }


//    public static Stream<Element> streamElementsDepthFirstPostOrder(Element start) {
//    	// ElementTransformer.transform(element, transform);
////    	 return Streams.stream(Traverser.forTree(ElementUtils::getSubElements).depthFirstPostOrder(start).iterator());
//    }


    /** TODO Move to a ContextUtils class */
    public static Context putAll(Context cxt, Map<String, String> map) {
        map.forEach((key, value) -> {
            String symbolName = MappingRegistry.mapPrefixName(key);
            Symbol symbol = Symbol.create(symbolName);
            cxt.set(symbol, value);
        });
        return cxt;
    }

    public static int sparqlIntegrate(CmdSparqlIntegrateMain cmd) throws Exception {
        int exitCode = 0; // success unless error

        JenaRuntime.isRDF11 = !cmd.useRdf10;

        CliUtils.configureGlobalSettings();

        // Set arq options
        putAll(ARQ.getContext(), cmd.arqOptions);

        if (cmd.explain) {
            ARQ.setExecutionLogging(InfoLevel.ALL);
        }


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
            operationalOut = StdIo.openStdOutWithCloseShield();
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

        long usedPrefixDefer = cmd.usedPrefixDefer;
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

            if (cmd.showAlgebra) {
                for (SparqlStmt sparqlStmt : clusterStmts) {
                    Op op = SparqlStmtUtils.toAlgebra(sparqlStmt);
                    if (op != null) {
                        logger.info("Algebra of " + sparqlStmt + ":\n" + op);
                    }
                }
            }

            SPARQLResultExProcessor effectiveHandler = SPARQLResultExProcessorBuilder.configureProcessor(
                    effOut, System.err,
                    outFormat,
                    clusterStmts,
                    prefixMapping,
                    tripleFormat,
                    quadFormat,
                    usedPrefixDefer,
                    jqMode, jqDepth, jqFlatMode,
                    effOut
                    );

            clusterToSink.put(filename, effectiveHandler);
        }



        // Start the engine (wrooom)

        RdfDataSource dataSourceTmp = setupRdfDataSource(cmd);

        if ("sansa".equalsIgnoreCase(cmd.dbLoader)) {
            logger.info("Using sansa loader for loading RDF files");
            dataSourceTmp = new RdfDataSourceFactorySansa().create(dataSourceTmp, null);
        }

        RdfDataSource datasetAndDelete = dataSourceTmp;;


        // Dataset dataset = datasetAndDelete.getKey();
        // Closeable deleteAction = datasetAndDelete.getValue();
        Thread shutdownHook = new Thread(() -> {
            try {
                datasetAndDelete.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);



        // Start all sinks
        for (SPARQLResultExProcessor handler : clusterToSink.values()) {
            handler.start();
        }

        try {
            Callable<RDFConnection> connSupp = () -> wrapWithAutoDisableReorder(datasetAndDelete.getConnection());

            try (RDFConnection serverConn = (cmd.server ? connSupp.call() : null)) {
                // RDFConnectionFactoryEx.getQueryConnection(conn)
                Server server = null;
                if (cmd.server) {
                    SparqlService sparqlService = FluentSparqlService.from(serverConn).create();

    //                Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
    //                        prefixMapping, false);// .getQueryParser();

                    int port = cmd.serverPort;
                    server = FactoryBeanSparqlServer.newInstance()
                            .setSparqlServiceFactory(() -> sparqlService.getRDFConnection())
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
            // Always rely on shutdown hook to close the engine/dataset

            // Don't close the dataset while the server is running - rely on the shutdown hook
//            if (!cmd.server) {
//                datasetAndDelete.close();
//            }
//            dataset.close();
//            deleteAction.close();

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


    /**
     * Essentially a wrapper for {@link SparqlStmtUtils#execAny(RDFConnection, SparqlStmt)} which
     * logs the workload (the sparql query) being processed.
     *
     * @param conn
     * @param workload
     * @param resultProcessor
     */
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



    /**
     * Checks whether transactions with the given transactional
     * are thread-independent.
     * For this purpose a txn is started on the calling thread. Afterwards,
     * the isInTransaction() flag is read from on a separate test thread.
     *
     */
    public boolean isTxnThreadIndependent(Transactional txn, TxnType txnType) {
        boolean status[] = Txn.calc(txn, txnType, () -> {
            boolean[] r = { false, false };
            r[0] = txn.isInTransaction();

            Thread x = new Thread(() -> r[1] = txn.isInTransaction());
            x.start();
            try {
                x.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return r;
        });

        boolean result = status[0] && status[1];
        return result;
    }
}
