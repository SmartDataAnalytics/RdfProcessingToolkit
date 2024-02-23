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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
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
import org.aksw.conjure.datasource.RdfDataSourceDecoratorSansa;
import org.aksw.jena_sparql_api.cache.advanced.RdfDataSourceWithRangeCache;
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
import org.aksw.jena_sparql_api.user_defined_function.UserDefinedFunctions;
import org.aksw.jenax.arq.picocli.CmdMixinArq;
import org.aksw.jenax.arq.util.dataset.HasDataset;
import org.aksw.jenax.arq.util.lang.RDFLanguagesEx;
import org.aksw.jenax.arq.util.query.QueryTransform;
import org.aksw.jenax.arq.util.security.ArqSecurity;
import org.aksw.jenax.arq.util.syntax.QueryUtils;
import org.aksw.jenax.arq.util.update.UpdateRequestUtils;
import org.aksw.jenax.arq.util.update.UpdateTransform;
import org.aksw.jenax.arq.util.update.UpdateUtils;
import org.aksw.jenax.dataaccess.sparql.connection.common.RDFConnectionUtils;
import org.aksw.jenax.dataaccess.sparql.dataengine.RdfDataEngine;
import org.aksw.jenax.dataaccess.sparql.datasource.RdfDataSource;
import org.aksw.jenax.dataaccess.sparql.exec.query.QueryExecWrapperBase;
import org.aksw.jenax.dataaccess.sparql.exec.query.QueryExecs;
import org.aksw.jenax.dataaccess.sparql.exec.update.UpdateExecWrapperBase;
import org.aksw.jenax.dataaccess.sparql.execution.update.UpdateProcessorWrapperBase;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RdfDataEngineFactory;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RdfDataEngineFactoryRegistry;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RdfDataEngines;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.RdfDataSourceSpecBasicFromMap;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.RdfDataSources;
import org.aksw.jenax.dataaccess.sparql.link.common.RDFLinkUtils;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RdfDataSourcePolyfill;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RdfDataSourceWithBnodeRewrite;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RdfDataSourceWithLocalCache;
import org.aksw.jenax.dataaccess.sparql.polyfill.datasource.RdfDataSourceWithLocalLateral;
import org.aksw.jenax.graphql.api.GraphQlExecFactory;
import org.aksw.jenax.graphql.sparql.GraphQlExecFactoryOverSparql;
import org.aksw.jenax.sparql.query.rx.RDFDataMgrEx;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.core.SparqlStmtMgr;
import org.aksw.jenax.stmt.core.SparqlStmtUpdate;
import org.aksw.jenax.stmt.resultset.SPARQLResultEx;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.aksw.jenax.web.server.boot.ServerBuilder;
import org.aksw.jenax.web.server.boot.ServletBuilderGraphQl;
import org.aksw.jenax.web.server.boot.ServletBuilderSparql;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.geosparql.spatial.SpatialIndex;
import org.apache.jena.irix.IRIx;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Model;
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
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprTransform;
import org.apache.jena.sparql.expr.ExprTransformCopy;
import org.apache.jena.sparql.function.user.UserDefinedFunctionDefinition;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateLoad;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.service.enhancer.init.ServiceEnhancerInit;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.system.Txn;
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

import jakarta.servlet.http.HttpServletRequest;

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

        // SparqlScriptProcessor processor = SparqlScriptProcessor.createWithEnvSubstitution(prefixMapping)

        // Union default graph transformation is now part of the connection
        boolean unionDefaultGraphOnCliArgs = false;
        if (unionDefaultGraphOnCliArgs && cmd.unionDefaultGraph) {
            processor.addPostTransformer(stmt -> {
                SparqlStmt r = SparqlStmtUtils.applyOpTransform(stmt,
                    op -> Transformer.transformSkipService(new TransformUnionQuery(), op));
                return r;
            });
        }

        List<String> args = cmd.nonOptionArgs;


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

        String outFormat = cmd.outFormat;
        if (outFormat == null) {
            // Try to derive the outFormat from the filename - if given.
            if (outFilename != null) {
                FileName fn = fileNameParser.parse(outFilename);
                outFormat = fn.getContentPart();
                rawOutEncodings.addAll(fn.getEncodingParts());
            }
        }

        CompressorStreamFactory csf = CompressorStreamFactory.getSingleton();

        OutputStream operationalOut;
        if (!Strings.isNullOrEmpty(outFilename)) {
            outFile = Paths.get(outFilename).toAbsolutePath();

            Path outDir = outFile.getParent();
            if (cmd.outMkDirs && outDir != null && !Files.exists(outFile)) {
                Files.createDirectories(outDir);
            }

            if(Files.exists(outFile) && !Files.isWritable(outFile)) {
                throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
            }

            if (outFormat == null) {
                FileName fileName = fileNameParser.parse(outFilename);
                String contentPart = fileName.getContentPart();

                Lang lang = RDFLanguagesEx.findLang(contentPart);
                rawOutEncodings.addAll(fileName.getEncodingParts());

                // fileName = FileNameUtils.deconstruct(outFilename);

                // Lang lang = RDFDataMgr.determineLang(outFilename, null, null);
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

            outEncodings = rawOutEncodings.stream().map(x -> ContentTypeUtils.getCodingExtensions().getAlternatives().get(x)).collect(Collectors.toList());
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

            outEncodings = rawOutEncodings.stream().map(x -> ContentTypeUtils.getCodingExtensions().getAlternatives().get(x)).collect(Collectors.toList());
            operationalOut = RDFDataMgrEx.encode(operationalOut, outEncodings, csf);

            outFile = null;
            tmpFile = null;
        }


        processor.process(args);



        Path splitFolder = cmd.splitFolder == null
                ? null
                : Paths.get(cmd.splitFolder);

        List<Entry<SparqlStmt, Provenance>> workloads = processor.getSparqlStmts();

        if (cmd.useIriAsGiven) {
            workloads = workloads.stream().map(e ->
                Map.entry(
                    SparqlStmtUtils.applyElementTransform(e.getKey(), ExprTransformIriToIriAsGiven::transformElt),
                    e.getValue()))
               .collect(Collectors.toList());
        }

        if (true) { // bnodeasgiven enabled by default because RML test cases (representing KG construction tooling) rely on it)
            workloads = workloads.stream().map(e ->
                Map.entry(
                    SparqlStmtUtils.applyElementTransform(e.getKey(), ExprTransformBNodeToBNodeAsGiven::transformElt),
                    e.getValue()))
               .collect(Collectors.toList());
        }

        // If loader is "update" then materialize all LOAD statements into INSERT DATA ones
        if ("insert".equalsIgnoreCase(cmd.dbLoader)) {
            UpdateTransform xform = update -> update instanceof UpdateLoad
                    ? UpdateUtils.materialize((UpdateLoad)update)
                    : update;

            workloads = workloads.stream().map(e -> {
                SparqlStmt stmt = e.getKey();
                if (stmt.isUpdateRequest()) {
                    UpdateRequest before = stmt.getUpdateRequest();
                    UpdateRequest after = UpdateRequestUtils.copyTransform(before, xform);
                    stmt = new SparqlStmtUpdate(after);
                }
                return Map.entry(stmt, e.getValue());
            })
            .collect(Collectors.toList());
        }


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

        // Start the engine

        Dataset datasetTmp = null;
        RdfDataEngine dataSourceTmp = setupRdfDataEngine(cmd);
        if (dataSourceTmp instanceof HasDataset) {
            datasetTmp = ((HasDataset) dataSourceTmp).getDataset();
            if (datasetTmp != null) {
                Context cxt = datasetTmp.getContext();
                if (cxt != null) {

                    if (!cmd.server || cmd.unsafe) {
                        cxt.setTrue(ArqSecurity.symAllowFileAccess);
                    }
                }
            }
        }

        // Auto transactions are handled by the DataEngineFactory implementations
//        if (datasetTmp != null) {
//            dataSourceTmp = RdfDataEngines.wrapWithAutoTxn(dataSourceTmp, datasetTmp);
//        }

        Dataset finalDataset = datasetTmp;

        if (cmd.arqConfig.geoindex) {
            if (finalDataset == null) {
                throw new RuntimeException("GeoIndex requested but the configured engine does not appear to be Jena-based as the dataset was null!");
            }
        }

        dataSourceTmp = RdfDataEngines.wrapWithQueryTransform(dataSourceTmp, null, QueryExecs::withDetailedHttpMessages);

        if (cmd.cachePath != null) {
            Path cachePathBase = Path.of(cmd.cachePath);
            Path parent = cachePathBase.getParent();
            if (parent != null && !Files.exists(parent)) {
                throw new RuntimeException("Cannot initialize cache because folder " + parent + " does not exist");
            }

            String datasetId = cmd.datasetId;
            if (datasetId == null) {
                datasetId = RdfDataSources.fetchDatasetHash(dataSourceTmp);
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

            dataSourceTmp = RdfDataEngines.transform(dataSourceTmp, ds -> RdfDataSourceWithRangeCache.create(ds, cachePath, cmd.dbMaxResultSize));

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
                dataSourceTmp = RdfDataEngines.wrapWithLinkTransform(dataSourceTmp, link ->
                        RDFLinkUtils.wrapWithQueryTransform(link, RdfDataSourceWithLocalCache.TransformInjectCacheSyntax::rewriteQuery, null));
                // dataSourceTmp = RdfDataEngines.of(new RdfDataSourceWithLocalCache(dataSourceTmp), dataSourceTmp);
            }
        }

        if ("sansa".equalsIgnoreCase(cmd.dbLoader)) {
            logger.info("Using sansa loader for loading RDF files");
            dataSourceTmp = RdfDataEngines.decorate(dataSourceTmp, new RdfDataSourceDecoratorSansa());
        }

        // Attempt to detect the dbms name.
        // If one is detected then use it as an active profile name.
        String dmbsProfile = RdfDataSources.compute(dataSourceTmp, RdfDataSourcePolyfill::detectProfile);
        if (logger.isInfoEnabled()) {
            logger.info("Detected DBMS: " + dmbsProfile);
        }

        String bnodeProfile = cmd.bnodeProfile;
        if ("auto".equalsIgnoreCase(bnodeProfile)) {
            bnodeProfile = dmbsProfile;
        }

        Set<String> macroProfiles = new HashSet<>();
        if (dmbsProfile != null) {
            macroProfiles.add(dmbsProfile);
        }

        if (!Strings.isNullOrEmpty(bnodeProfile)) {
            dataSourceTmp = RdfDataEngines.of(
                    new RdfDataSourceWithBnodeRewrite(dataSourceTmp, bnodeProfile),
                    dataSourceTmp::close);
            // RdfDataSourceDecorator decorator = (x, conf) -> new RdfDataSourceWithBnodeRewrite(x, bnodeProfile);
            // dataSourceTmp = RdfDataEngines.decorate(dataSourceTmp, decorator);
        }

        // Load function macros (run sparql inferences first)
        Map<String, UserDefinedFunctionDefinition> udfRegistry = new LinkedHashMap<>();
        for (String macroSource : cmd.macroSources) {
            Model model = RDFDataMgr.loadModel(macroSource);
            SparqlStmtMgr.execSparql(model, "udf-inferences.sparql");
            Map<String, UserDefinedFunctionDefinition> contrib = UserDefinedFunctions.load(model, macroProfiles);
            udfRegistry.putAll(contrib);
        }

        if (!cmd.macroSources.isEmpty()) {
            logger.info("Loaded functions: {}", udfRegistry.keySet());
            logger.info("Loaded {} function definitions from  {} macro sources.", udfRegistry.size(), cmd.macroSources.size());
            // ExprTransform eform = new ExprTransformExpand(udfRegistry);
            ExprTransform eform = new ExprTransformCopy() {
                @Override
                public Expr transform(ExprFunctionN func, ExprList args) {
                    // XXX Could avoid func.copy()
                    return UserDefinedFunctions.expandMacro(udfRegistry, func.copy(args));
                }
            };
            QueryTransform qform = q -> QueryUtils.rewrite(q, op -> Transformer.transform(null, eform, op));
            dataSourceTmp = RdfDataEngines.wrapWithQueryTransform(dataSourceTmp, qform, null);
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
            RdfDataSource[] dataSourceTmp2 = new RdfDataSource[1];
            dataSourceTmp2[0] = () -> {
                RDFConnection ca = RDFConnectionUtils.wrapWithAutoDisableReorder(datasetAndDelete.getConnection());

                RDFConnection cb = RDFConnectionUtils.wrapWithContextMutator(ca, cxt -> {
                    SparqlIntegrateCmdImpls.configureOptimizer(cxt);
                    RdfDataSource thisDataSource = dataSourceTmp2[0];
                    cxt.put(RDFLinkUtils.symRdfDataSource, thisDataSource);
                });

                RDFConnection cbx = cmd.unionDefaultGraph
                        ? RDFConnectionUtils.wrapWithStmtTransform(cb, stmt -> SparqlStmtUtils.applyOpTransform(stmt,
                                op -> Transformer.transformSkipService(new TransformUnionQuery(), op)))
                        : cb;

                // TODO Add a util method wrapWithStmtTransform
                RDFConnection cc = RDFConnectionUtils.wrapWithQueryTransform(
                    cbx,
                    null, queryExec -> {
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
                                        updateSpatialIndex(finalDataset);

                                        // The the spatial index symbol is in the dataset's context
                                        // copy it into the query exec's context.
                                        // TODO This is hacky; can we avoid the copy?
                                        Context execCxt = getContext();
                                        if (execCxt != null) {
                                            execCxt.set(SpatialIndex.SPATIAL_INDEX_SYMBOL,
                                                    finalDataset.getContext().get(SpatialIndex.SPATIAL_INDEX_SYMBOL));
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
                                        updateSpatialIndex(finalDataset);

                                        // The the spatial index symbol is in the dataset's context
                                        // copy it into the query exec's context.
                                        // Context execCxt = getContext();
                                        Context execCxt = finalDatasetCxt;
                                        if (execCxt != null) {
                                            execCxt.set(SpatialIndex.SPATIAL_INDEX_SYMBOL,
                                                    finalDataset.getContext().getAsString(SpatialIndex.SPATIAL_INDEX_SYMBOL));
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
                                    updateSpatialIndex(finalDataset);
                                }
                            }
                        };
                    }
                    return r;
                });

                return cd;
            };

            RdfDataSource dataSource = dataSourceTmp2[0];

            // TODO Make this configurable
            boolean clientSideConstructQuads = false;
            if (clientSideConstructQuads) {
                dataSource = RdfDataSources.execQueryViaSelect(dataSource, query -> query.isConstructQuad());
            }

            if (cmd.polyfillLateral) {
                dataSource = RdfDataSourceWithLocalLateral.wrap(dataSource);
            }

            RdfDataSource finalDataSource = dataSource;

            // RDFConnectionFactoryEx.getQueryConnection(conn)
            Server server = null;
            if (cmd.server) {
                // SparqlService sparqlService = FluentSparqlService.from(serverConn).create();

//                Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
//                        prefixMapping, false);// .getQueryParser();

                RdfDataSource serverDataSource = () -> {
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

                GraphQlExecFactory graphQlExecFactory = cmd.graphQlAutoConfigure
                    ? GraphQlExecFactoryOverSparql.autoConfLazy(serverDataSource)
                    : GraphQlExecFactoryOverSparql.of(serverDataSource);

                int port = cmd.serverPort;
                server = ServerBuilder.newBuilder()
                        .addServletBuilder(ServletBuilderSparql.newBuilder()
                            .setSparqlServiceFactory((HttpServletRequest httpRequest) -> serverDataSource.getConnection())
                            .setSparqlStmtParser(
                                    //SparqlStmtParser.wrapWithOptimizePrefixes(
                                    processor.getSparqlParser()
                                    //)
                            )
                        )
                        .addServletBuilder(ServletBuilderGraphQl.newBuilder()
                            .setGraphQlExecFactory(graphQlExecFactory)
                        )
                        .setPort(port).create();

                server.start();

                URI browseUri = new URI("http://localhost:" + port + "/sparql");
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(browseUri);
                    } catch (UnsupportedOperationException e) {
                        logger.info("Note: Could not open system browser.");
                    }
                }
                logger.info("SPARQL service running at: " + browseUri);
            }

            try (RDFConnection conn = finalDataSource.getConnection()) {
                for (Entry<SparqlStmt, Provenance> stmtEntry : workloads) {
                    Provenance prov = stmtEntry.getValue();
                    String clusterId = splitFolder == null ? "" : Optional.ofNullable(prov.getSourceLocalName()).orElse("");

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

    /** Be careful not to call within a read transaction! */
    public static void updateSpatialIndex(Dataset dataset) {
        Context cxt = dataset.getContext();
        logger.info("(Re-)computing geo index");
        try {
            GeoSPARQLConfig.setupSpatialIndex(dataset);
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
        boolean isPatternless = updateRequest == null
                ? false // Robustness; should never be null
                : updateRequest.getOperations().stream()
                    .allMatch(update -> {
                        boolean patternless = false;
                        if (update instanceof UpdateLoad) {
                            patternless = true;
                        } else if (update instanceof UpdateData) {
                            patternless = true;
                        } else if (update instanceof UpdateLoad) {
                            UpdateModify um = (UpdateModify)update;
                            patternless = um.getWherePattern() == null;
                        }
                        return patternless;
                    })
                ;
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
