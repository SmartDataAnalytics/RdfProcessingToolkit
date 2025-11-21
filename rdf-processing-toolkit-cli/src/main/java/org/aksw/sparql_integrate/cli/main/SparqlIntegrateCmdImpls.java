package org.aksw.sparql_integrate.cli.main;

import java.awt.Desktop;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aksw.commons.io.util.StdIo;
import org.aksw.commons.util.string.FileName;
import org.aksw.commons.util.string.FileNameParser;
import org.aksw.jena_sparql_api.algebra.expr.transform.ExprTransformConstantFoldWithIris;
import org.aksw.jena_sparql_api.algebra.expr.transform.ExprTransformVirtualBnodeUris.BnodeRewriteMode;
import org.aksw.jena_sparql_api.cache.advanced.RDFLinkSourceWithRangeCache;
import org.aksw.jena_sparql_api.conjure.utils.ContentTypeUtils;
import org.aksw.jena_sparql_api.delay.extra.Delayer;
import org.aksw.jena_sparql_api.delay.extra.DelayerDefault;
import org.aksw.jena_sparql_api.rx.io.resultset.OutputFormatSpec;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExProcessor;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExProcessorBuilder;
import org.aksw.jena_sparql_api.rx.io.resultset.SPARQLResultExVisitor;
import org.aksw.jena_sparql_api.rx.script.MultiException;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor.Provenance;
import org.aksw.jena_sparql_api.sparql.ext.url.E_IriAsGiven.ExprTransformIriToIriAsGiven;
import org.aksw.jena_sparql_api.sparql.ext.url.F_BNodeAsGiven.ExprTransformBNodeToBNodeAsGiven;
import org.aksw.jena_sparql_api.sparql.ext.url.JenaUrlUtils;
import org.aksw.jenax.arq.picocli.CmdMixinArq;
import org.aksw.jenax.arq.util.security.ArqSecurity;
import org.aksw.jenax.arq.util.update.UpdateRequestUtils;
import org.aksw.jenax.arq.util.update.UpdateTransform;
import org.aksw.jenax.arq.util.update.UpdateUtils;
import org.aksw.jenax.arq.util.var.Vars;
import org.aksw.jenax.dataaccess.sparql.connection.common.RDFConnectionUtils;
import org.aksw.jenax.dataaccess.sparql.creator.RDFDatabase;
import org.aksw.jenax.dataaccess.sparql.creator.RDFDatabaseBuilder;
import org.aksw.jenax.dataaccess.sparql.creator.RDFDatabaseFactory;
import org.aksw.jenax.dataaccess.sparql.datasource.RDFDataSource;
import org.aksw.jenax.dataaccess.sparql.engine.RDFEngine;
import org.aksw.jenax.dataaccess.sparql.engine.RDFEngines;
import org.aksw.jenax.dataaccess.sparql.exec.query.QueryExecWrapperBase;
import org.aksw.jenax.dataaccess.sparql.exec.query.QueryExecs;
import org.aksw.jenax.dataaccess.sparql.exec.update.UpdateExecWrapperBase;
import org.aksw.jenax.dataaccess.sparql.execution.update.UpdateProcessorWrapperBase;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.DecoratedRDFEngine;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RDFEngineBuilder;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RDFEngineDecorator;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RDFEngineFactory;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RDFEngineFactoryLegacyBase;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RDFEngineFactoryLegacyBase.CloseablePath;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RDFEngineFactoryRegistry;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.ExprTransformPrettyMacroExpansion;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.RDFDataSources;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.RdfDataSourceDecorator;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.RdfDataSourceSpecBasicFromMap;
import org.aksw.jenax.dataaccess.sparql.link.common.RDFLinkUtils;
import org.aksw.jenax.dataaccess.sparql.link.query.LinkSparqlQueryTransformPaginate;
import org.aksw.jenax.dataaccess.sparql.link.transform.RDFLinkTransforms;
import org.aksw.jenax.dataaccess.sparql.linksource.RDFLinkSource;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RdfDataSourcePolyfill;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RDFDataSourceWithBnodeRewrite;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RDFDataSourceWithLocalCache;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RdfDataSourceWithLocalLateral;
import org.aksw.jenax.graphql.rdf.api.RdfGraphQlExecFactory;
import org.aksw.jenax.graphql.sparql.GraphQlExecFactoryOverSparql;
import org.aksw.jenax.graphql.sparql.v2.exec.api.high.GraphQlExecFactory;
import org.aksw.jenax.graphql.sparql.v2.rewrite.TransformHarmonizeTentris;
import org.aksw.jenax.graphql.sparql.v2.schema.SchemaNavigator;
import org.aksw.jenax.graphql.util.GraphQlUtils;
import org.aksw.jenax.model.udf.util.UserDefinedFunctions;
import org.aksw.jenax.sparql.query.rx.RDFDataMgrEx;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.core.SparqlStmtMgr;
import org.aksw.jenax.stmt.core.SparqlStmtUpdate;
import org.aksw.jenax.stmt.resultset.SPARQLResultEx;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.aksw.jenax.web.server.boot.ServerBuilder;
import org.aksw.jenax.web.server.boot.ServletBuilderGraphQl;
import org.aksw.jenax.web.server.boot.ServletBuilderGraphQlV2;
import org.aksw.jenax.web.server.boot.ServletBuilderSparql;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.aksw.sparql_integrate.web.servlet.ServletGraphQlSchema;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.geosparql.spatial.index.v2.SpatialIndexIoKryo;
import org.apache.jena.geosparql.spatial.index.v2.SpatialIndexLib;
import org.apache.jena.graph.Node;
import org.apache.jena.irix.IRIx;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdflink.RDFConnectionAdapter;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformUnionQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.Optimize;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.QueryExecBuilderAdapter;
import org.apache.jena.sparql.expr.ExprTransform;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.function.user.UserDefinedFunctionDefinition;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateLoad;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.service.enhancer.init.ServiceEnhancerInit;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.system.Txn;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.BaseEncoding;

import graphql.language.AstPrinter;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;

public class SparqlIntegrateCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateCmdImpls.class);

    public static RDFDatabaseFactory setupRdfDatabaseBuilder(CmdSparqlIntegrateMain cmd) throws Exception {
        String sourceType = Optional.ofNullable(cmd.engine).orElse("mem");
        RDFDatabaseFactory factory = RDFEngineFactoryRegistry.get().getDatabaseFactory(sourceType);
        return factory;
    }

    public static String getEffectiveEngine(String engine) {
        return Optional.ofNullable(engine).orElse("mem");
    }

    public static CloseablePath setupDbFolder(CmdSparqlIntegrateMain cmd) throws IOException {
        String sourceType = getEffectiveEngine(cmd.engine);
        RDFEngineFactory factory = RDFEngineFactoryRegistry.get().getFactory(sourceType);
        if (factory == null) {
            throw new RuntimeException("No RDFEngineFactory registered under name " + sourceType);
        }

        CloseablePath result = RDFEngineFactoryLegacyBase.setupPath(cmd.dbPath, cmd.dbFs, cmd.tempPath, cmd.engine, !cmd.dbKeep);
        return result;
    }

    public static RDFEngineBuilder<?> setupRdfDataEngineBuilder(CmdSparqlIntegrateMain cmd) throws Exception {
        String sourceType = getEffectiveEngine(cmd.engine);
        RDFEngineFactory factory = RDFEngineFactoryRegistry.get().getFactory(sourceType);
        if (factory == null) {
            throw new RuntimeException("No RdfDataSourceFactory registered under name " + sourceType);
        }

        RDFEngineBuilder<?> engineBuilder = factory.newBuilder();

        RdfDataSourceSpecBasicFromMap spec = RdfDataSourceSpecBasicFromMap.create();
        spec.setTempDir(cmd.tempPath);
        // FIXME Removed auto-delete flag from the engine - its related to the database builder.
        spec.setAutoDeleteIfCreated(!cmd.dbKeep);
        spec.setLocation(cmd.dbPath);
        spec.setLocationContext(cmd.dbFs);

        spec.getMap().putAll(cmd.dbOptions);

        engineBuilder.setProperties(spec.getMap());

        // RdfDataEngine result = factory.create(spec.getMap());
        return engineBuilder;
    }

    public static final Symbol SPATIAL_INDEX_IS_CLEAN = Symbol.create("http://jena.apache.org/spatial#indexIsClean");

    public static int sparqlIntegrate(CmdSparqlIntegrateMain cmd) throws Exception {
        int exitCode = 0; // success unless error

        CmdMixinArq.configureGlobal(cmd.arqConfig);
        CmdMixinArq.configureCxt(ARQ.getContext(), cmd.arqConfig);

//        JenaRuntime.isRDF11 = !cmd.useRdf10;
//
//        CliUtils.configureGlobalSettings();
//
//        if (cmd.geoindex) {
//            System.setProperty("jena.geosparql.skip", String.valueOf(false));
//            //InitGeoSPARQL.start();
//            new InitGeoSPARQL().start();
//            GeoSPARQLConfig.setupNoIndex();
//        }
//
//        // Automatically load external javascript functions from functions.js unless specified
//        Symbol jsLibrarySym = Symbol.create(MappingRegistry.mapPrefixName("arq:js-library"));
//        ARQ.getContext().setIfUndef(jsLibrarySym, "functions.js");
//
//        // Set arq options
//        ContextUtils.putAll(ARQ.getContext(), cmd.arqOptions);

//        if (cmd.explain) {
//            ARQ.setExecutionLogging(InfoLevel.ALL);
//        }

        Stopwatch sw = Stopwatch.createStarted();

        PrefixMapping prefixMapping = CliUtils.configPrefixMapping(cmd);

        Function<String, String> lookup = key -> {
            String r = null;
            if (cmd.env != null) {
                r = cmd.env.get(key);
            }
            if (r == null) {
                r = System.getenv(key);
            }
            return r;
        };

        SparqlScriptProcessor processor = SparqlScriptProcessor.createWithEnvSubstitution(prefixMapping, lookup);

        // SparqlScriptProcessor processor =
        // SparqlScriptProcessor.createWithEnvSubstitution(prefixMapping)

        // Union default graph transformation is now part of the connection
        boolean unionDefaultGraphOnCliArgs = false;
        if (unionDefaultGraphOnCliArgs && cmd.unionDefaultGraph) {
            processor.addPostTransformer(stmt -> {
                SparqlStmt r = SparqlStmtUtils.applyOpTransform(stmt,
                        op -> Transformer.transformSkipService(new TransformUnionQuery(), op));
                return r;
            });
        }

        List<String> rawArgs = cmd.nonOptionArgs;

        List<String> args = new ArrayList<>();

        for (String rawArg : rawArgs) {
            List<String> resolved = ClassPathResourceResolver.resolve(rawArg);
            if (resolved.isEmpty()) {
                args.add(rawArg);
            } else {
                logger.info("Resolved " + rawArg + " to " + resolved);
                args.addAll(resolved);
            }
        }

        // If an in/out file is given then prepend it to the arguments
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

        // Separate encoding and content type from the format and/or filename

        List<String> rawOutEncodings = new ArrayList<>();
        List<String> outEncodings;

        FileNameParser fileNameParser = FileNameParser.of(
                x -> ContentTypeUtils.getCtExtensions().getAlternatives().containsKey(x.toLowerCase()),
                x -> ContentTypeUtils.getCodingExtensions().getAlternatives().containsKey(x.toLowerCase()));

        // If a format is given it takes precedence - otherwise try to use the filename
        String outFormat = cmd.outFormat;

        // Format source can be either a filename "foo.nt", a file extension "ttl" or a
        // format string "turtle/pretty"
        String formatSource = outFormat == null ? outFilename : outFormat;

        // Try to parse the format source as a file name or file extension
        if (formatSource != null) {
            // Try to derive the outFormat from the filename - if given.
            FileName fn = fileNameParser.parse(formatSource);
            String tmpOutFormat = fn.getContentPart();

            // Replace the outFormat with the result of the successful parse
            if (tmpOutFormat != null) {
                outFormat = tmpOutFormat;
            }

            rawOutEncodings.addAll(fn.getEncodingParts());
        }

        CompressorStreamFactory csf = CompressorStreamFactory.getSingleton();

        OutputStream operationalOut;
        if (!Strings.isNullOrEmpty(outFilename)) {
            outFile = Paths.get(outFilename).toAbsolutePath();

            Path outDir = outFile.getParent();
            if (cmd.outMkDirs && outDir != null && !Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }

            if (Files.exists(outFile) && !Files.isWritable(outFile)) {
                throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
            }

//            if (outFormat == null) {
//                FileName fileName = fileNameParser.parse(outFilename);
//                String contentPart = fileName.getContentPart();
//
//                Lang lang = RDFLanguagesEx.findLang(contentPart);
//                rawOutEncodings.addAll(fileName.getEncodingParts());
//
//                // fileName = FileNameUtils.deconstruct(outFilename);
//
//                // Lang lang = RDFDataMgr.determineLang(outFilename, null, null);
//                if (lang != null) {
//                    RDFFormat fmt = RDFWriterRegistry.defaultSerialization(lang);
//                    outFormat = fmt == null ? null : fmt.toString();
//                    logger.info("Inferred output format from " + outFilename + ": " + outFormat);
//                } else {
//                    throw new RuntimeException("Failed to determine output format");
//                }
//            }

            Path parent = outFile.getParent();
            String tmpName = "." + outFile.getFileName().toString() + ".tmp";
            tmpFile = parent.resolve(tmpName);

            operationalOut = Files.newOutputStream(tmpFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            outEncodings = rawOutEncodings.stream()
                    .map(x -> ContentTypeUtils.getCodingExtensions().getAlternatives().get(x))
                    .collect(Collectors.toList());
            operationalOut = RDFDataMgrEx.encode(operationalOut, outEncodings, csf);

            OutputStream finalOut = operationalOut;
            // Register a shutdown hook to delete the temporary file
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    finalOut.close();
                    Files.deleteIfExists(tmpFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));

        } else {
            operationalOut = StdIo.openStdOutWithCloseShield();

            outEncodings = rawOutEncodings.stream()
                    .map(x -> ContentTypeUtils.getCodingExtensions().getAlternatives().get(x))
                    .collect(Collectors.toList());
            operationalOut = RDFDataMgrEx.encode(operationalOut, outEncodings, csf);

            outFile = null;
            tmpFile = null;
        }

        processor.process(args);

        Path splitFolder = cmd.splitFolder == null ? null : Paths.get(cmd.splitFolder);

        List<Entry<SparqlStmt, Provenance>> workloads = processor.getSparqlStmts();

        if (cmd.useIriAsGiven) {
            workloads = workloads.stream().map(e -> Map.entry(
                    SparqlStmtUtils.applyElementTransform(e.getKey(), ExprTransformIriToIriAsGiven::transformElt),
                    e.getValue())).collect(Collectors.toList());
        }

        if (true) { // bnodeasgiven enabled by default because RML test cases (representing KG
                    // construction tooling) rely on it)
            workloads = workloads.stream()
                    .map(e -> Map.entry(SparqlStmtUtils.applyElementTransform(e.getKey(),
                            ExprTransformBNodeToBNodeAsGiven::transformElt), e.getValue()))
                    .collect(Collectors.toList());
        }

        // If loader is "update" then materialize all LOAD statements into INSERT DATA
        // ones
        if ("insert".equalsIgnoreCase(cmd.dbLoader)) {
            UpdateTransform xform = update -> update instanceof UpdateLoad
                    ? UpdateUtils.materialize((UpdateLoad) update)
                    : update;

            workloads = workloads.stream().map(e -> {
                SparqlStmt stmt = e.getKey();
                if (stmt.isUpdateRequest()) {
                    UpdateRequest before = stmt.getUpdateRequest();
                    UpdateRequest after = UpdateRequestUtils.copyTransform(before, xform);
                    stmt = new SparqlStmtUpdate(after);
                }
                return Map.entry(stmt, e.getValue());
            }).collect(Collectors.toList());
        }

        // If the engine features a database builder then use it
        // to load all static files (before queries or updates)

        // If there is a database builder, then RPT will provision a target folder
        // based on the configuration and delete its fileset after database shutdown.
        CloseablePath closeablePath = null;

            // formatSource, outFilename, outFormat, formatSource, unionDefaultGraphOnCliArgs);

        RDFDatabaseFactory databaseFactory = setupRdfDatabaseBuilder(cmd);
        RDFDatabase database = null;

        if (databaseFactory != null) {
            closeablePath = setupDbFolder(cmd);

            RDFDatabaseBuilder<?> databaseBuilder = databaseFactory.newBuilder();

            databaseBuilder.setProperties(cmd.dbLoaderOptions);

            Path outputFolder = closeablePath.path();
            databaseBuilder.setOutputFolder(outputFolder);

            List<UpdateLoad> loads = new ArrayList<>();
            int stmtIdx = 0;
            for (Entry<SparqlStmt, Provenance> e : workloads) {
                SparqlStmt stmt = e.getKey();
                if (stmt.isUpdateRequest()) {
                    UpdateRequest ur = stmt.getUpdateRequest();
                    List<Update> updates = ur.getOperations();
                    boolean allLoad = updates.stream().allMatch(x -> x instanceof UpdateLoad);
                    if (allLoad) {
                        updates.stream().map(x -> (UpdateLoad) x).forEach(loads::add);
                        ++stmtIdx;
                        continue;
                    }
                }
                break;
            }
            // Remove workloads that were shifted to the optimized loader
            workloads.subList(0, stmtIdx).clear();

            for (UpdateLoad load : loads) {
                String src = load.getSource();
                Node dest = load.getDest();
                databaseBuilder.addPath(src, dest);
            }
            database = databaseBuilder.build();
        }

        RDFEngineBuilder<?> engineBuilder = setupRdfDataEngineBuilder(cmd);

        if (database != null) {
            engineBuilder.setDatabase(database);
        }

        RDFEngine rdfEngine = engineBuilder.build();
        RDFEngineDecorator<?> rdfEngineDecorator = RDFEngines.decorate(rdfEngine);

        // If we created a database ourselves, then register a delete action with the engine
        // database != null -> closablePath != null
        if (database != null && !cmd.dbKeep) {
            // RdfDataEngine tmp = dataSourceTmp;
            RDFDatabase tmp = database;
            Closeable deletePath = closeablePath.closeable();
            rdfEngineDecorator.addCloseAction(() -> {
                tmp.getFileSet().delete();
                deletePath.close();
            });
        }

        // Workloads clustered by their split target filenames
        // If there are no splits then there is one cluster whose key is the empty
        // string
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
                OutputFormatSpec spec = OutputFormatSpec.create(outFormat, tripleFormat, quadFormat, clusterStmts,
                        jqMode);
                String fileExt = spec.getFileExtension();
                fileExt = fileExt == null ? "dat" : fileExt;

                String baseFilename = com.google.common.io.Files.getNameWithoutExtension(filename);
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

            SPARQLResultExProcessor effectiveHandler = SPARQLResultExProcessorBuilder.configureProcessor(effOut,
                    System.err, outFormat, clusterStmts, prefixMapping, tripleFormat, quadFormat, usedPrefixDefer,
                    jqMode, jqDepth, jqFlatMode, effOut);

            clusterToSink.put(filename, effectiveHandler);
        }

        // Start the engine

        DatasetGraph datasetTmp = rdfEngine.getLinkSource().getDatasetGraph();
        // if (rdfEngineDecorator instanceof HasDataset) {
        // datasetTmp = ((HasDataset) rdfEngineDecorator).getDataset();
        if (datasetTmp != null) {
            Context cxt = datasetTmp.getContext();
            if (cxt != null) {

                if (!cmd.server || cmd.unsafe) {
                    cxt.setTrue(ArqSecurity.symAllowFileAccess);
                }
            }
        }
        // }

        // Auto transactions are handled by the DataEngineFactory implementations
//        if (datasetTmp != null) {
//            dataSourceTmp = RdfDataEngines.wrapWithAutoTxn(dataSourceTmp, datasetTmp);
//        }

        Dataset finalDataset = datasetTmp == null ? null : DatasetFactory.wrap(datasetTmp);

        if (cmd.arqConfig.geoindex) {
            if (finalDataset == null) {
                throw new RuntimeException(
                        "GeoIndex requested but the configured engine does not appear to be Jena-based as the dataset was null!");
            }

            // If there is an existing index then try to load it.
            Path geoIndexFile = cmd.arqConfig.geoindexFile;
            if (geoIndexFile != null) {
                GeoSPARQLConfig.setupSpatialIndex(finalDataset, cmd.arqConfig.geoindexSrs, geoIndexFile);
                finalDataset.getContext().setTrue(SPATIAL_INDEX_IS_CLEAN);
            }
        }

        Long resultSetPageSize = cmd.paginationConfig.queryPageSize;
        if (resultSetPageSize != null && resultSetPageSize > 0) {
            rdfEngineDecorator.decorate(new LinkSparqlQueryTransformPaginate(resultSetPageSize));
        }

        Long queryLimit = cmd.paginationConfig.queryLimit;
        if (queryLimit != null && queryLimit > 0) {
            rdfEngineDecorator.decorate(RDFLinkTransforms.withLimit(queryLimit));
        }

        rdfEngineDecorator = rdfEngineDecorator.decorate(QueryExecs::withDetailedHttpMessages);

        if (cmd.cachePath != null) {
            Path cachePathBase = Path.of(cmd.cachePath);
            Path parent = cachePathBase.getParent();
            if (parent != null && !Files.exists(parent)) {
                throw new RuntimeException("Cannot initialize cache because folder " + parent + " does not exist");
            }

            String datasetId = cmd.datasetId;
            if (datasetId == null) {
                // TODO Supplying of the connection from the decorator is hacky
                RDFEngineDecorator<?> tmpDecorator = rdfEngineDecorator;
                datasetId = RDFDataSources.fetchDatasetHash(() -> RDFConnectionAdapter.adapt(tmpDecorator.snapshotLink()));
                if (logger.isInfoEnabled()) {
                    logger.info("Automatically derived datasetId using data sampling: " + datasetId);
                    logger.info("Use '--dataset-id your-id' to configure the datasetId manually.");
                }
            }

            if (datasetId.isEmpty()) {
                datasetId = "unknown";
            }

            // Sanitize XXX Maybe use some better method?
            datasetId = BaseEncoding.base64Url().encode(datasetId.getBytes());
            Path cachePath = cachePathBase.resolve(datasetId);

            if (logger.isInfoEnabled()) {
                logger.info("DatasetId: " + datasetId);
                logger.info("Cache folder: " + cachePath);
            }

//            HashCode datasetHashCode;
//            try {
//            	byte[] decoded = BaseEncoding.base64Url().decode(datasetId);
//            	datasetHashCode = HashCode.fromBytes(decoded);
//            } catch (Exception e) {
//            	logger.info("Could not decode datasetId as a ");
//
//            }

            rdfEngineDecorator = rdfEngineDecorator.decorate(
                    (RDFLinkSource linkSource) -> RDFLinkSourceWithRangeCache.create(linkSource, cachePath, cmd.dbMaxResultSize));

//            RdfDataEngines.wrapWithCustomQueryExecBuilder(dataSourceTmp, ds -> new QueryExecBuilderCustomBase<QueryExecBuilder>() {
//            });
//
//            QueryExecFactoryQueryTransform decorizer = QueryExecFactoryQueryRangeCache
//                    .createQueryExecMod(cachePath, cmd.dbMaxResultSize);
//
//            QueryExecutionFactory i = QueryExecutionFactories.of(dataSourceTmp);
//            QueryExecFactory j = QueryExecFactories.adapt(i);
//            QueryExecFactory k = QueryExecFactories.adapt(decorizer.apply(j));
//            QueryExecutionFactory l = QueryExecutionFactories.adapt(k);
//            dataSourceTmp = RdfDataEngines.adapt(l);

            if (cmd.cacheRewriteGroupBy) {
                rdfEngineDecorator = rdfEngineDecorator.decorate(
                    RDFDataSourceWithLocalCache.TransformInjectCacheSyntax::rewriteQuery);
                // dataSourceTmp = RdfDataEngines.of(new
                // RdfDataSourceWithLocalCache(dataSourceTmp), dataSourceTmp);
            }
        }

        if ("sansa".equalsIgnoreCase(cmd.dbLoader)) {
            logger.info("Using sansa loader for loading RDF files");

            // Load sansa dynamically as to not require it to be present on the classpath
            RdfDataSourceDecorator sansaDecorator = (RdfDataSourceDecorator) Class
                    .forName("org.aksw.conjure.datasource.RdfDataSourceDecoratorSansa").getConstructor().newInstance();
            // dataSourceTmp = RdfDataEngines.decorate(dataSourceTmp, new
            // RdfDataSourceDecoratorSansa());
            rdfEngineDecorator = rdfEngineDecorator.decorate((RDFLinkSource ds) -> sansaDecorator.decorate(ds.asDataSource(), null).asLinkSource());
        }

        SchemaNavigator graphqlSchemaNavigator = null;
        String graphQlSchemaPrettyStr = null;
        if (cmd.graphQlSchema != null) {
            StreamManager streamMgr = StreamManager.get();
            Parser parser = new Parser();
            String metaSchemaRawStr = toStringUtf8(streamMgr, "jenax.meta.gqls");
            Document metaDoc = parser.parseDocument(metaSchemaRawStr);

            String graphQlSchemaRawStr = toStringUtf8(streamMgr, cmd.graphQlSchema);
            Document schemaDoc = GraphQlUtils.parseUnrestricted(graphQlSchemaRawStr);

            List<Definition> mergedDefinitions = new ArrayList<>();
            mergedDefinitions.addAll(metaDoc.getDefinitions());
            mergedDefinitions.addAll(schemaDoc.getDefinitions());

            // Create a new merged document
            Document mergedDoc = Document.newDocument().definitions(mergedDefinitions).build();

            mergedDoc = GraphQlUtils.applyTransform(mergedDoc, new TransformHarmonizeTentris());
            // GraphQlUtils.println(System.out, schemaDoc);
            graphQlSchemaPrettyStr = AstPrinter.printAst(mergedDoc);

            // System.out.println(schemaDoc); //GraphQlUtils.toString(schemaDoc));
            // System.out.println("========");

            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry schema = schemaParser.buildRegistry(mergedDoc);

            graphqlSchemaNavigator = SchemaNavigator.of(schema);
        }

        DecoratedRDFEngine<?> datasetAndDelete = rdfEngineDecorator.build();

        // Dataset dataset = datasetAndDelete.getKey();
        // Closeable deleteAction = datasetAndDelete.getValue();

        // TODO Deregister the shutdown hook when the engine gets closed - because the hook is then no longer needed.
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
            RDFDataSource[] dataSourceTmp2 = new RDFDataSource[1];
            dataSourceTmp2[0] = () -> {
                RDFConnection c0 = datasetAndDelete.getLinkSource().asDataSource().getConnection();
                RDFConnection ca = RDFConnectionUtils.wrapWithAutoDisableReorder(c0);

                RDFConnection cb = RDFConnectionUtils.wrapWithContextMutator(ca, cxt -> {
                    SparqlIntegrateCmdImpls.configureOptimizer(cxt);
                    RDFDataSource thisDataSource = dataSourceTmp2[0];
                    cxt.put(RDFLinkUtils.symRdfDataSource, thisDataSource);
                });

                RDFConnection cbx = cmd.unionDefaultGraph
                        ? RDFConnectionUtils
                                .wrapWithStmtTransform(cb,
                                        stmt -> SparqlStmtUtils.applyOpTransform(stmt,
                                                op -> Transformer.transformSkipService(new TransformUnionQuery(), op)))
                        : cb;

                // TODO Add a util method wrapWithStmtTransform
                RDFConnection cc = RDFConnectionUtils.wrapWithQueryTransform(cbx, null, queryExec -> {
                    QueryExec r = queryExec;

                    Duration delayDuration = cmd.delay;
                    if (!delayDuration.isZero()) {
                        r = new QueryExecWrapperBase<>(r) {
                            @Override
                            public void beforeExec() {
                                Delayer delayer = DelayerDefault.createFromNow(delayDuration.toMillis());
                                try {
                                    delayer.doDelay();
                                } catch (InterruptedException e) {
                                    // XXX Should we fail on interrupt?
                                }
                                super.beforeExec();
                            }
                        };
                    }

                    if (cmd.showAlgebra) {
                        r = new QueryExecWrapperBase<>(r) {
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

                    // r = QueryExecWrapperTxn.wrap(r, cb);

                    if (cmd.arqConfig.geoindex) {

                        // Spatial index creation must happen outside of a transaction!
                        r = new QueryExecWrapperBase<>(r) {
                            @Override
                            public void beforeExec() {
                                Context finalDatasetCxt = finalDataset.getContext();
                                if (finalDatasetCxt != null && finalDatasetCxt.isFalseOrUndef(SPATIAL_INDEX_IS_CLEAN)) {
                                    updateSpatialIndex(finalDataset, cmd.arqConfig.geoindexSrs, cmd.arqConfig.geoindexFile);

                                    // The the spatial index symbol is in the dataset's context
                                    // copy it into the query exec's context.
                                    // TODO This is hacky; can we avoid the copy?
                                    Context execCxt = getContext();
                                    if (execCxt != null) {
                                        SpatialIndexLib.setSpatialIndex(execCxt,
                                            SpatialIndexLib.getSpatialIndex(finalDataset.getContext()));
//                                        execCxt.set(SpatialIndexConstants.SPATIAL_INDEX_SYMBOL,
//                                                finalDataset.getContext().get(SpatialIndexConstants.SPATIAL_INDEX_SYMBOL));
                                    }
                                }
                            }
                        };
                    }
                    return r;
                });

                RDFConnection cd = RDFConnectionUtils.wrapWithUpdateTransform(cc, null, (ur, up) -> {
                    UpdateProcessor r = up;
                    if (cmd.showAlgebra) {
                        r = new UpdateProcessorWrapperBase<>(up) {
                            @Override
                            public void beforeExec() {
                                if (ur != null) {
                                    Op op = SparqlStmtUtils.toAlgebra(new SparqlStmtUpdate(ur));
                                    if (op != null) {
                                        // Context cxt = getContext();
                                        Context cxt = finalDataset.getContext();
                                        if (cxt != null) {
                                            op = Optimize.optimize(op, cxt);
                                        }
                                        if (op != null) {
                                            if (logger.isInfoEnabled()) {
                                                logger.info("Algebra of " + ur + ":\n" + op);
                                            }
                                        }
                                    }
                                }
                            }
                        };
                    }

                    // r = UpdateProcessorWrapperTxn.wrap(r, cb);

                    if (cmd.arqConfig.geoindex) {
                        r = new UpdateExecWrapperBase<>(r) {
                            protected boolean spatialUpdateNeeded = false;

                            @Override
                            public void beforeExec() {
                                Context finalDatasetCxt = finalDataset.getContext();
                                if (finalDatasetCxt != null && finalDatasetCxt.isFalseOrUndef(SPATIAL_INDEX_IS_CLEAN)) {
                                    spatialUpdateNeeded = isSpatialIndexUpdateImmediatelyRequired(ur);
                                    if (spatialUpdateNeeded) {
                                        updateSpatialIndex(finalDataset, cmd.arqConfig.geoindexSrs, cmd.arqConfig.geoindexFile);

                                        // The the spatial index symbol is in the dataset's context
                                        // copy it into the query exec's context.
                                        // Context execCxt = getContext();
                                        Context execCxt = finalDatasetCxt;
                                        if (execCxt != null) {
                                            SpatialIndexLib.setSpatialIndex(execCxt,
                                                SpatialIndexLib.getSpatialIndex(finalDataset.getContext()));
//                                            execCxt.set(SpatialIndexUtils.SPATIAL_INDEX_SYMBOL, finalDataset.getContext()
//                                                    .getAsString(SpatialIndexUtils.SPATIAL_INDEX_SYMBOL));
                                        }
                                    }
                                }
                            }

                            @Override
                            public void afterExec() {
                                // Always mark index dirty after update
                                finalDataset.getContext().setFalse(SPATIAL_INDEX_IS_CLEAN);

                                // Defer update
                                if (false && spatialUpdateNeeded) {
                                    // Note: The update runs in the same transaction as the update!
                                    updateSpatialIndex(finalDataset, cmd.arqConfig.geoindexSrs, cmd.arqConfig.geoindexFile);
                                }
                            }
                        };
                    }
                    return r;
                });

                return cd;
            };

            RDFDataSource dataSource = dataSourceTmp2[0];

            // TODO Make this configurable
            boolean clientSideConstructQuads = false;
            if (clientSideConstructQuads) {
                dataSource = RDFDataSources.execQueryViaSelect(dataSource, query -> query.isConstructQuad());
            }

            if (cmd.polyfillLateral != null) {
                dataSource = RdfDataSourceWithLocalLateral.wrap(dataSource, cmd.polyfillLateral);
            }

            if (cmd.polyfillConstantFold) {
                dataSource = RDFDataSources.decorate(dataSource, new ExprTransformConstantFoldWithIris());
            }

            dataSource = applyMacroExpansion(cmd, dataSource);

            RDFDataSource finalDataSource = dataSource;

            // RDFConnectionFactoryEx.getQueryConnection(conn)
            Server server = null;
            if (cmd.server) {
                // SparqlService sparqlService = FluentSparqlService.from(serverConn).create();

//                Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
//                        prefixMapping, false);// .getQueryParser();

                RDFDataSource serverDataSource = () -> {
                    RDFConnection r = finalDataSource.getConnection();
                    if (cmd.readOnlyMode) {
                        r = RDFConnectionUtils.wrapWithQueryOnly(r);
                    }
//                    try {
//                        RDFConnection spatialRdfConnection = getSpatialRdfConnection(cmd, finalDataset, r, true);
//                        r = spatialRdfConnection;
//                    } catch (SpatialIndexException e) {
//                        logger.error("Error encountered", e);
//                    }
                    return r;
                };

                RdfGraphQlExecFactory graphQlExecFactory = cmd.graphQlAutoConfigure
                        ? GraphQlExecFactoryOverSparql.autoConfLazy(serverDataSource)
                        : GraphQlExecFactoryOverSparql.of(serverDataSource);

                GraphQlExecFactory graphQlExecFactoryV2 = GraphQlExecFactory
                        .of(() -> QueryExecBuilderAdapter.adapt(serverDataSource.newQuery()), graphqlSchemaNavigator);

                int port = cmd.serverPort;
                ServerBuilder serverBuilder = ServerBuilder.newBuilder()
                        .addServletBuilder(ServletBuilderSparql.newBuilder()
                                .setSparqlServiceFactory(
                                        (HttpServletRequest httpRequest) -> serverDataSource.getConnection())
                                .setSparqlStmtParser(
                                        // SparqlStmtParser.wrapWithOptimizePrefixes(
                                        processor.getSparqlParser()
                                // )
                                ))
                        .addServletBuilder(ServletBuilderGraphQl.newBuilder().setGraphQlExecFactory(graphQlExecFactory))
                        .addServletBuilder(
                                ServletBuilderGraphQlV2.newBuilder().setGraphQlExecFactory(graphQlExecFactoryV2))
                        .addServletBuilder(ServletLdvConfigJs.newBuilder().setDbEngine(cmd.engine)).setPort(port);

                if (graphqlSchemaNavigator != null) {
                    serverBuilder = serverBuilder.addServletBuilder(ServletGraphQlSchema.newBuilder()
                            .setContent(graphQlSchemaPrettyStr).setContentType(MediaType.TEXT_PLAIN));
                }

                server = serverBuilder.create();

                server.start();

                // Try to get the host address from a network device (e.g. within a docker
                // container)
                String hostAddress;
                try (final DatagramSocket socket = new DatagramSocket()) {
                    socket.connect(InetAddress.getByName("1.1.1.1"), 53);
                    hostAddress = socket.getLocalAddress().getHostAddress();
                } catch (Exception e) {
                    // Fall back to localhost
                    hostAddress = "localhost";
                }
                URI browseUri = new URI("http://" + hostAddress + ":" + port + "/");
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI("http://localhost:" + port));
                    } catch (UnsupportedOperationException e) {
                        logger.info("Note: Could not open system browser.");
                    }
                }
                logger.info("RdfProcessingToolkit Server running at: " + browseUri);
                logger.info("SPARQL service running at: " + browseUri + "sparql");
            }

            try (RDFConnection conn = finalDataSource.getConnection()) {
                for (Entry<SparqlStmt, Provenance> stmtEntry : workloads) {
                    Provenance prov = stmtEntry.getValue();
                    String clusterId = splitFolder == null ? ""
                            : Optional.ofNullable(prov.getSourceLocalName()).orElse("");

                    SPARQLResultExProcessor sink = clusterToSink.get(clusterId);
//                    RDFConnection connB = getSpatialRdfConnection(cmd, dataset, conn, stmtEntry.getKey() instanceof SparqlStmtQuery);
                    try {
                        execStmt(conn, stmtEntry, sink);
                    } catch (Exception e) {
                        String message = "Error encountered; trying to continue but exit code will be non-zero";
                        if (cmd.isDebugMode()) {
                            logger.error(message, e);
                        } else {
                            logger.error(message + ": " + MultiException.getEMessage(e));
                        }
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
            if (outFile != null) {
                Files.move(tmpFile, outFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("SPARQL overall execution finished after " + sw.stop());

            if (server != null) {
                logger.info("Server still running on port " + cmd.serverPort + ". Terminate with CTRL+C");
                server.join();
            }

        } finally {
            // Always rely on shutdown hook to close the engine/dataset

            // Don't close the dataset while the server is running - rely on the shutdown
            // hook
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

    public static RDFDataSource applyMacroExpansion(CmdSparqlIntegrateMain cmd, RDFDataSource dataSourceTmp) {
        // Attempt to detect the dbms name.
        // If one is detected then use it as an active profile name.
        String dbmsName = RDFDataSources.compute(dataSourceTmp, RdfDataSourcePolyfill::detectProfile);
        if (logger.isInfoEnabled()) {
            logger.info("Detected DBMS: " + dbmsName);
        }

        String dbmsProfile = dbmsName == null ? null : "http://ns.aksw.org/profile/" + dbmsName;

        String bnodeProfile = cmd.bnodeProfile;
        if ("auto".equalsIgnoreCase(bnodeProfile)) {
            bnodeProfile = dbmsProfile;
        }

        Set<String> macroProfiles = new HashSet<>();
        if (dbmsProfile != null) {
            macroProfiles.add(dbmsProfile);
        }

        BnodeRewriteMode bnodeRewriteMode = BnodeRewriteMode.LOOKUP_ONLY;

        if (!Strings.isNullOrEmpty(bnodeProfile)) {
            dataSourceTmp = new RDFDataSourceWithBnodeRewrite(dataSourceTmp, bnodeProfile, BnodeRewriteMode.FULL);

//            dataSourceTmp = RdfDataEngines.of(new RdfDataSourceWithBnodeRewrite(dataSourceTmp, bnodeProfile),
//                    dataSourceTmp::close);
            // RdfDataSourceDecorator decorator = (x, conf) -> new
            // RdfDataSourceWithBnodeRewrite(x, bnodeProfile);
            // dataSourceTmp = RdfDataEngines.decorate(dataSourceTmp, decorator);
        } else {
            if (dbmsProfile != null) {
                dataSourceTmp = new RDFDataSourceWithBnodeRewrite(dataSourceTmp, bnodeProfile, BnodeRewriteMode.LOOKUP_ONLY);
            }

            // Replace <http://ns.aksw.org/function/forceBnodeIri> with a default definition
            // TODO The UDF system lacks a fallback profile feature!
            // TODO HashMap wrapping needed because immutable map causes NPE in Jena's ExprTransformExpand with functions that do not have an IRI such as coalesce.
            String bnodeFnIri = "http://ns.aksw.org/function/forceBnodeIri";
            Map<String, UserDefinedFunctionDefinition> udfRegistry = new HashMap<>(Map.of(
                bnodeFnIri,
                new UserDefinedFunctionDefinition(bnodeFnIri, new ExprVar(Vars.x), List.of(Vars.x))));

            ExprTransform exprTransform = new ExprTransformPrettyMacroExpansion(udfRegistry);
            dataSourceTmp = RDFDataSources.decorate(dataSourceTmp, exprTransform);
        }

        // Load function macros (run sparql inferences first)
        Map<String, UserDefinedFunctionDefinition> udfRegistry = new LinkedHashMap<>();

        // XXX There should be a separate registry for default macros to load.
        loadMacros(macroProfiles, udfRegistry, "macros/ollama.ttl");

        for (String macroSource : cmd.macroSources) {
            loadMacros(macroProfiles, udfRegistry, macroSource);
        }

        if (!cmd.macroSources.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Loaded functions: {}", udfRegistry.keySet());
                logger.info("Loaded {} function definitions from  {} macro sources.", udfRegistry.size(),
                        cmd.macroSources.size());
            }
            // ExprTransform eform = new ExprTransformExpand(udfRegistry);
            ExprTransform eform = new ExprTransformPrettyMacroExpansion(udfRegistry);
            dataSourceTmp = RDFDataSources.decorate(dataSourceTmp, eform);
            // dataSourceTmp = RdfDataSourceTransforms.of(eform).apply(dataSourceTmp);
        }
        return dataSourceTmp;
    }

    private static void loadMacros(Set<String> macroProfiles, Map<String, UserDefinedFunctionDefinition> udfRegistry,
            String macroSource) {
        Model model = RDFDataMgr.loadModel(macroSource);
        SparqlStmtMgr.execSparql(model, "udf-inferences.rq");
        Map<String, UserDefinedFunctionDefinition> contrib = UserDefinedFunctions.load(model, macroProfiles);
        udfRegistry.putAll(contrib);
    }

    /** Be careful not to call within a read transaction! */
    public static void updateSpatialIndex(Dataset dataset, String srs, Path file) {
        Context cxt = dataset.getContext();
        // logger.info("(Re-)computing geo index");
        try {
            SpatialIndexIoKryo.buildSpatialIndex(dataset, srs, file);
            cxt.setTrue(SPATIAL_INDEX_IS_CLEAN);
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("no srs found")) {
                // ignore - assuming simply no geodata present
            } else {
                logger.error("Failed to udpate geo index after update", e);
            }
        }
    }

    /** Returns true if the update request mentions a where pattern */
    public static boolean isSpatialIndexUpdateImmediatelyRequired(UpdateRequest updateRequest) {
        // If the update request is only LOAD statements then
        // do not build it immediately
        boolean isPatternless = updateRequest == null ? false // Robustness; should never be null
                : updateRequest.getOperations().stream().allMatch(update -> {
                    boolean patternless = false;
                    if (update instanceof UpdateLoad) {
                        patternless = true;
                    } else if (update instanceof UpdateData) {
                        patternless = true;
                    } else if (update instanceof UpdateLoad) {
                        UpdateModify um = (UpdateModify) update;
                        patternless = um.getWherePattern() == null;
                    }
                    return patternless;
                });
        return !isPatternless;
    }
//    private static RDFConnection getSpatialRdfConnection(CmdSparqlIntegrateMain cmd, Dataset dataset, RDFConnection conn, boolean compute) throws SpatialIndexException {
//        if (cmd.arqConfig.geoindex && dataset != null && compute) {
//            logger.info("Computing geo index");
//            GeoSPARQLConfig.setupSpatialIndex(dataset);
//            Object spatialIndex = dataset.getContext().get(SpatialIndex.SPATIAL_INDEX_SYMBOL);
//            return RDFConnectionUtils.wrapWithContextMutator(conn, (ctx) -> ctx.put(SpatialIndex.SPATIAL_INDEX_SYMBOL, spatialIndex));
//            //GeoSPARQLConfig.setupSpatialIndex(dataSourceTmp.getConnection().fetchDataset());
//        }
//        return conn;
//    }

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
     * Essentially a wrapper for
     * {@link SparqlStmtUtils#execAny(RDFConnection, SparqlStmt)} which logs the
     * workload (the sparql query) being processed.
     *
     * @param conn
     * @param workload
     * @param resultProcessor
     */
    public static void execStmt(RDFConnection conn, Entry<? extends SparqlStmt, ? extends Provenance> workload,
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
                try (SPARQLResultEx sr = SparqlStmtUtils.execAny(conn, stmt, cxtMutator)) {
                    resultProcessor.forwardEx(sr);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
//        }

    }

    /**
     * Checks whether transactions with the given transactional are
     * thread-independent. For this purpose a txn is started on the calling thread.
     * Afterwards, the isInTransaction() flag is read from on a separate test
     * thread.
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

    public static String toStringUtf8(StreamManager streamMgr, String resourceName) throws IOException {
        String result;
        try (InputStream in = streamMgr.open(resourceName)) {
            result = IOUtils.toString(in, StandardCharsets.UTF_8);
        }
        return result;
    }
}

//if (cmd.arqConfig.geoindex) {
//  if (finalDataset == null) {
//      throw new RuntimeException("GeoIndex requested but the configured engine does not appear to be Jena-based as the dataset was null!");
//  } else {
//      dataSource = RdfDataSources.decorateUpdate(dataSource, (UpdateExec ue) -> {
//          return new UpdateExecWrapperBase<>(ue) {
//              @Override
//              protected void afterExec() {
//                  finalDataset.getContext().setFalse(SPATIAL_INDEX_IS_CLEAN);
//                  // Note: The update runs in the same transaction as the update!
//                  updateSpatialIndexIfDirty(finalDataset);
//              }
//          };
//      });
//
//      dataSource = RdfDataSources.decorateQueryBeforeTxnBegin(dataSource, () -> {
//          Txn.executeWrite(finalDataset, () -> {
//              updateSpatialIndexIfDirty(finalDataset);
//          });
//      });
//  }
//}
//
//boolean alwaysUseTxn = true;
//if (alwaysUseTxn) {
//  dataSource = RdfDataSources.applyLinkTransform(dataSource, link -> {
//      RDFLink r = link;
//      r = RDFLinkUtils.wrapWithQueryTransform(r, null, (QueryExec qe) -> QueryExecWrapperTxn.wrap(qe, link));
//      r = RDFLinkUtils.wrapWithUpdateTransform(r, null, (ur, ue) -> UpdateExecWrapperTxn.wrap(ue, link));
//      return r;
//  });
//}
