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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.aksw.commons.io.util.StdIo;
import org.aksw.conjure.datasource.RdfDataSourceDecoratorSansa;
import org.aksw.jena_sparql_api.cache.advanced.QueryExecFactoryQueryRangeCache;
import org.aksw.jena_sparql_api.rx.io.resultset.OutputFormatSpec;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExProcessor;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExProcessorBuilder;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExVisitor;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor.Provenance;
import org.aksw.jena_sparql_api.sparql.ext.url.JenaUrlUtils;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactories;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.aksw.jenax.arq.connection.core.RDFConnectionUtils;
import org.aksw.jenax.arq.connection.link.QueryExecFactories;
import org.aksw.jenax.arq.connection.link.QueryExecFactory;
import org.aksw.jenax.arq.connection.link.QueryExecFactoryQueryDecorizer;
import org.aksw.jenax.arq.datasource.HasDataset;
import org.aksw.jenax.arq.datasource.RdfDataEngineFactory;
import org.aksw.jenax.arq.datasource.RdfDataEngineFactoryRegistry;
import org.aksw.jenax.arq.datasource.RdfDataEngines;
import org.aksw.jenax.arq.datasource.RdfDataSourceSpecBasicFromMap;
import org.aksw.jenax.arq.util.security.ArqSecurity;
import org.aksw.jenax.connection.dataengine.RdfDataEngine;
import org.aksw.jenax.connection.query.QueryExecDecoratorBase;
import org.aksw.jenax.connection.query.QueryExecDecoratorTxn;
import org.aksw.jenax.connection.query.QueryExecs;
import org.aksw.jenax.connection.update.UpdateProcessorDecoratorBase;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.core.SparqlStmtParser;
import org.aksw.jenax.stmt.core.SparqlStmtQuery;
import org.aksw.jenax.stmt.core.SparqlStmtUpdate;
import org.aksw.jenax.stmt.resultset.SPARQLResultEx;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.aksw.jenax.web.server.boot.FactoryBeanSparqlServer;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.jena.JenaRuntime;
import org.apache.jena.ext.com.google.common.base.Stopwatch;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.geosparql.spatial.SpatialIndex;
import org.apache.jena.geosparql.spatial.SpatialIndexException;
import org.apache.jena.irix.IRIx;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
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
import org.apache.jena.sparql.algebra.optimize.Optimize;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.mgt.Explain.InfoLevel;
import org.apache.jena.sparql.service.enhancer.init.ServiceEnhancerInit;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.MappingRegistry;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.system.Txn;
import org.apache.jena.update.UpdateProcessor;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class SparqlIntegrateCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateCmdImpls.class);

    public static RdfDataEngine setupRdfDataEngine(CmdSparqlIntegrateMain cmd) throws Exception {

        String sourceType = Optional.ofNullable(cmd.engine).orElse("mem");

        RdfDataEngineFactory factory = RdfDataEngineFactoryRegistry.get().getFactory(sourceType);
        if (factory == null) {
            throw new RuntimeException("No RdfDataSourceFactory registered under name " + sourceType);
        }

        RdfDataSourceSpecBasicFromMap spec = RdfDataSourceSpecBasicFromMap.create();
        spec.setTempDir(cmd.tempPath);
        spec.setAutoDeleteIfCreated(!cmd.dbKeep);
        spec.setLocation(cmd.dbPath);
        spec.setLocationContext(cmd.dbFs);

        spec.getMap().putAll(cmd.dbOptions);

        RdfDataEngine result = factory.create(spec.getMap());
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
                } else {
                    throw new RuntimeException("Failed to determine output format");
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

            clusters = Multimaps.index(workloads, item -> item.getValue().getSourceLocalName());
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

//            if (cmd.showAlgebra) {
//                for (SparqlStmt sparqlStmt : clusterStmts) {
//                    Op op = SparqlStmtUtils.toAlgebra(sparqlStmt);
//                    if (op != null) {
//                        logger.info("Algebra of " + sparqlStmt + ":\n" + op);
//                    }
//                }
//            }

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

        Dataset dataset = null;
        RdfDataEngine dataSourceTmp = setupRdfDataEngine(cmd);
        if (dataSourceTmp instanceof HasDataset) {
            dataset = ((HasDataset) dataSourceTmp).getDataset();

            if (dataset != null) {
                Context cxt = dataset.getContext();
                if (cxt != null) {

                    if (!cmd.server || cmd.unsafe) {
                        cxt.setTrue(ArqSecurity.symAllowFileAccess);
                    }
                }
            }

        }
        dataSourceTmp = RdfDataEngines.wrapWithQueryTransform(dataSourceTmp, null, QueryExecs::withDetailedHttpMessages);

        if (cmd.cachePath != null) {
            Path cachePath = Path.of(cmd.cachePath);
            Path parent = cachePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                throw new RuntimeException("Folder " + parent + " does not exist");
            }

            QueryExecFactoryQueryDecorizer decorizer = QueryExecFactoryQueryRangeCache
                    .createQueryExecMod(cachePath, cmd.dbMaxResultSize);

            QueryExecutionFactory i = QueryExecutionFactories.of(dataSourceTmp);
            QueryExecFactory j = QueryExecFactories.adapt(i);
            QueryExecFactory k = QueryExecFactories.adapt(decorizer.apply(j));
            QueryExecutionFactory l = QueryExecutionFactories.adapt(k);
            dataSourceTmp = RdfDataEngines.adapt(l);
        }



        if ("sansa".equalsIgnoreCase(cmd.dbLoader)) {
            logger.info("Using sansa loader for loading RDF files");
            dataSourceTmp = RdfDataEngines.decorate(dataSourceTmp, new RdfDataSourceDecoratorSansa());
        }

        RdfDataEngine datasetAndDelete = dataSourceTmp;


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

        // QueryExecutionFactoryRangeCache.decorate(null, splitFolder, jqDepth);

        try {
            Supplier<RDFConnection> connSupp = () -> {
                RDFConnection ca = RDFConnectionUtils.wrapWithAutoDisableReorder(datasetAndDelete.getConnection());

                RDFConnection cb = RDFConnectionUtils.wrapWithContextMutator(ca, SparqlIntegrateCmdImpls::configureOptimizer);

                // TODO Add a util method wrapWithStmtTransform
                RDFConnection cc = RDFConnectionUtils.wrapWithQueryTransform(
                    cb,
                    null, queryExec -> {
                        QueryExec r = queryExec;
                        if (cmd.showAlgebra) {
                            r = new QueryExecDecoratorBase<>(r) {
                                @Override
                                public void beforeExec() {
                                    Query q = getQuery();
                                    if (q != null) {
                                        Op op = Algebra.compile(q); // SparqlStmtUtils.toAlgebra(sparqlStmt);
                                        if (op != null) {
                                            Context cxt = getContext();
                                            if (cxt != null) {
                                                op = Optimize.optimize(op, cxt);
                                            }
                                            if (op != null) {
                                                logger.info("Algebra of " + q + ":\n" + op);
                                            }
                                        }
                                    }
                                }
                            };
                        }

                        r = QueryExecDecoratorTxn.wrap(r, cb);
                        return r;
                    });

                RDFConnection cd = RDFConnectionUtils.wrapWithUpdateTransform(cc, null, (ur, up) -> {
                    UpdateProcessor r = up;
                    if (cmd.showAlgebra) {
                        r = new UpdateProcessorDecoratorBase<>(up) {
                            @Override
                            public void beforeExec() {
                                if (ur != null) {
                                    Op op = SparqlStmtUtils.toAlgebra(new SparqlStmtUpdate(ur));
                                    if (op != null) {
                                        Context cxt = getContext();
                                        if (cxt != null) {
                                            op = Optimize.optimize(op, cxt);
                                        }
                                        if (op != null) {
                                            logger.info("Algebra of " + ur + ":\n" + op);
                                        }
                                    }
                                }
                            }
                        };
                    }
                    return r;
                    // r = UpdateExecDeco
                });

                return cd;
            };


            // RDFConnectionFactoryEx.getQueryConnection(conn)
            Server server = null;
            if (cmd.server) {
                // SparqlService sparqlService = FluentSparqlService.from(serverConn).create();

//                Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
//                        prefixMapping, false);// .getQueryParser();

                Dataset finalDataset = dataset;
                Supplier<RDFConnection> serverConnSupp = () -> {
                    RDFConnection r = connSupp.get();
                    if (cmd.readOnlyMode) {
                        r = RDFConnectionUtils.wrapWithQueryOnly(r);
                    }
                    try {
                        RDFConnection spatialRdfConnection = getSpatialRdfConnection(cmd, finalDataset, r, true);
                        r = spatialRdfConnection;
                    } catch (SpatialIndexException e) {
                        logger.error("Error encountered", e);
                    }
                    return r;
                };

                int port = cmd.serverPort;
                server = FactoryBeanSparqlServer.newInstance()
                        .setSparqlServiceFactory((HttpServletRequest httpRequest) -> serverConnSupp.get())
                        .setSparqlStmtParser(
                                //SparqlStmtParser.wrapWithOptimizePrefixes(
                                        processor.getSparqlParser()
                                //)
                        )
                        .setPort(port).create();

                server.start();

                URI browseUri = new URI("http://localhost:" + port + "/sparql");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(browseUri);
                } else {
                    logger.info("SPARQL service with in-memory result dataset running at " + browseUri);
                }
            }

            if (cmd.geoindex && dataset == null) {
                logger.warn("Cannot compute geo index with non data-set connection");
            }
            try (RDFConnection conn = connSupp.get()) {

                for (Entry<SparqlStmt, Provenance> stmtEntry : workloads) {
                    Provenance prov = stmtEntry.getValue();
                    String clusterId = splitFolder == null ? "" : Optional.ofNullable(prov.getSourceLocalName()).orElse("");

                    SPARQLResultExProcessor sink = clusterToSink.get(clusterId);
                    RDFConnection connB = getSpatialRdfConnection(cmd, dataset, conn, stmtEntry.getKey() instanceof SparqlStmtQuery);
                    try {
                        execStmt(connB, stmtEntry, sink);
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

    private static RDFConnection getSpatialRdfConnection(CmdSparqlIntegrateMain cmd, Dataset dataset, RDFConnection conn, boolean compute) throws SpatialIndexException {
        if (cmd.geoindex && dataset != null && compute) {
            logger.info("Computing geo index");
            GeoSPARQLConfig.setupSpatialIndex(dataset);
            Object spatialIndex = dataset.getContext().get(SpatialIndex.SPATIAL_INDEX_SYMBOL);
            return RDFConnectionUtils.wrapWithContextMutator(conn, (ctx) -> ctx.put(SpatialIndex.SPATIAL_INDEX_SYMBOL, spatialIndex));
            //GeoSPARQLConfig.setupSpatialIndex(dataSourceTmp.getConnection().fetchDataset());
        }
        return conn;
    }


    // This needs to be do on the connection's context
    public static void configureOptimizer(Context cxt) {

        ServiceEnhancerInit.wrapOptimizer(cxt);
        // ServicePlugins.wrapOptimizer(cxt);

//    	RewriteFactory baseFactory = Optional.<RewriteFactory>ofNullable(cxt.get(ARQConstants.sysOptimizerFactory))
//    			.orElse(Optimize.stdOptimizationFactory);
//    	RewriteFactory enhancedFactory = c -> {
//    		Rewrite baseRewrite = baseFactory.create(c);
//    		return op -> {
//    			Op a = Transformer.transform(new TransformJoinStrategyServiceSpecial(), op);
//    			return baseRewrite.rewrite(a);
//    		};
//    	};
//    	cxt.set(ARQConstants.sysOptimizerFactory, enhancedFactory);

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

            String sourceNamespace = prov.getSourceNamespace();
            IRIx irix = sourceNamespace == null ? null : IRIx.create(sourceNamespace);
            // Always update the context because it may be scoped by the
            // connection and thus be shared between requests
            Consumer<Context> cxtMutator = cxt -> {
                cxt.set(JenaUrlUtils.symContentBaseIriX, irix);
            };

            // Some RdfDataSource decorators will try to perform certain update operations
            // (e.g. loading a file) using parallel update requests. In that case
            // we must avoid to start a write txn that exclusively locks the dataset or
            // we will deadlock.
            boolean runUpdateWithAdhocTxn = false;

            if (runUpdateWithAdhocTxn && stmt.isUpdateRequest()) {
                Context cxt = ARQ.getContext().copy();
                cxtMutator.accept(cxt);
                conn.newUpdate().update(stmt.getUpdateRequest()).context(cxt).execute();
                // conn.update(stmt.getUpdateRequest());
            } else {
                Txn.exec(conn, txnType, () -> {
                    try(SPARQLResultEx sr = SparqlStmtUtils.execAny(conn, stmt, cxtMutator)) {
                        resultProcessor.forwardEx(sr);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
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
