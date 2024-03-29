package org.aksw.named_graph_stream.cli.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;

import org.aksw.commons.io.syscall.sort.SysSort;
import org.aksw.commons.io.util.StdIo;
import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.io.hdt.JenaPluginHdt;
import org.aksw.jena_sparql_api.rx.io.resultset.NamedGraphStreamCliUtils;
import org.aksw.jenax.arq.dataset.orderaware.DatasetFactoryEx;
import org.aksw.jenax.arq.util.lang.RDFLanguagesEx;
import org.aksw.jenax.sparql.query.rx.RDFDataMgrEx;
import org.aksw.jenax.sparql.query.rx.RDFDataMgrRx;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.core.SparqlStmtParserImpl;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParser;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParserImpl;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsCat;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsFilter;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsHead;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsMap;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsMerge;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsProbe;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsSort;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsSubjects;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsTail;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsUntil;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsWc;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsWhile;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;

/**
 * Implementation of all named graph stream commands as static methods.
 *
 *
 * @author raven
 *
 */
public class NgsCmdImpls {
    // FIXME Clean this up (use spring boot?)
    static { CliUtils.configureGlobalSettings(); }

    public static Collection<Lang> quadLangs = Arrays.asList(Lang.TRIG, Lang.NQUADS);
    public static Collection<Lang> tripleLangs = Arrays.asList(Lang.TURTLE, JenaPluginHdt.LANG_HDT);


    private static final Logger logger = LoggerFactory.getLogger(NgsCmdImpls.class);

    public static int cat(CmdNgsCat cmdCat) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdCat.outFormat);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdCat.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);
        return 0;
    }


    public static int filter(CmdNgsFilter cmdFilter) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdFilter.outFormat);

        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.get());
        SparqlStmt stmt = stmtParser.apply(cmdFilter.sparqlCondition);
        Query query = stmt.getQuery();

        Predicate<Dataset> tmpCondition = MainCliNamedGraphStream.createPredicate(query);

        Predicate<Dataset> condition = cmdFilter.drop ? tmpCondition.negate() : tmpCondition;

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdFilter.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs)
                .filter(condition::test);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);
        return 0;
    }


    public static int head(CmdNgsHead cmdHead) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdHead.outFormat);

        // parse the numRecord option
        Entry<Boolean, Long> e = cmdHead.numRecords;
        boolean negated = e.getKey();
        long val = e.getValue();

        if (negated) {
            throw new RuntimeException("Excluding the last n items not yet supported");
        }


        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdHead.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs)
            .take(val);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);

        return 0;
    }

    public static int tail(CmdNgsTail cmdTail) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdTail.outFormat);

        Entry<Boolean, Long> e = cmdTail.numRecords;
        boolean negated = e.getKey();
        long val = e.getValue();
        if(!negated) {
            throw new RuntimeException("Currently only skipping (via ngs tail -n +123) is supported");
        }

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdTail.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs)
            .skip(val);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);

        return 0;
    }

    /**
     * Quad-based mapping (rather than Dataset-based).
     *
     * @param cmdMap
     */
    public static int mapQuads(CmdNgsMap cmdMap) throws Exception {
        Iterable<Lang> probeLangs = RDFLanguagesEx.getQuadAndTripleLangs();

        List<String> args = NamedGraphStreamCliUtils.preprocessArgs(cmdMap.nonOptionArgs);
        Map<String, Callable<TypedInputStream>> map = NamedGraphStreamCliUtils.validate(args, probeLangs, true);

        String graphIri = cmdMap.mapSpec.graph;
        Node g = NodeFactory.createURI(graphIri);

        Function<Quad, Quad> quadMapper = q -> new Quad(g, q.asTriple());

        Flowable<Quad> quadFlow = Flowable.fromIterable(map.entrySet())
                .flatMap(arg -> {
                    String argName = arg.getKey();
                    logger.info("Loading stream for arg " + argName);
                    Callable<TypedInputStream> inSupp = arg.getValue();
                    Flowable<Quad> r = RDFDataMgrRx.createFlowableQuads(inSupp)
                    // TODO Decoding of distinguished names should go into the util method
                        .map(quad -> quadMapper.apply(quad));
                    return r;
                });



//        Flowable<Quad> quadFlow = Flowable.fromIterable(args)
//            .concatMap(arg -> {
//                Flowable<Quad> r = RDFDataMgrRx.createFlowableQuads(() ->
//                    RDFDataMgrEx.open(arg, probeLangs))
//                .map(quad -> quadMapper.apply(quad));
//                return r;
//            });

        RDFDataMgrRx.writeQuads(quadFlow, StdIo.openStdOutWithCloseShield(), RDFFormat.NQUADS);
            //.forEach(q -> RDFDataMgr.writeQuads(StdIo.openStdOutWithCloseShield(), Collections.singleton(q).iterator()));
//            .forEach(q -> NQuadsWriter.write(StdIo.openStdOutWithCloseShield(), Collections.singleton(q).iterator()));

        return 0;
    }


    public static int map(CmdNgsMap cmdMap) throws Exception {
        if (cmdMap.mapSpec.defaultGraph) {
            cmdMap.mapSpec.graph = Quad.defaultGraphIRI.toString();
        }

        if (cmdMap.mapSpec.graph != null) {
            mapQuads(cmdMap);
        } else {
            // NamedGraphStreamOps.map(MainCliNamedGraphStream.pm, cmdMap, StdIo.openStdOutWithCloseShield());
            execMap(MainCliNamedGraphStream.pm, cmdMap);
        }

        return 0;
    }



    public static void execMap(PrefixMapping pm, CmdNgsMap cmdFlatMap) {

        NamedGraphStreamCliUtils.execMap(pm,
                cmdFlatMap.nonOptionArgs,
                quadLangs,
                cmdFlatMap.mapSpec.stmts,
                cmdFlatMap.serviceTimeout,
                cmdFlatMap.outFormat,
                20);

//        String timeoutSpec = cmdFlatMap.serviceTimeout;
//        Consumer<Context> contextHandler = cxt -> {
//            if (!Strings.isNullOrEmpty(timeoutSpec)) {
//                cxt.set(Service.queryTimeout, timeoutSpec);
//            }
//        };
//
//        SparqlScriptProcessor scriptProcessor = SparqlScriptProcessor.createWithEnvSubstitution(pm);
//
//        // Register a (best-effort) union default graph transform
//        scriptProcessor.addPostTransformer(stmt -> SparqlStmtUtils.applyOpTransform(stmt,
//                op -> Transformer.transformSkipService(new TransformUnionQuery(), op)));
//
//
//        scriptProcessor.process(cmdFlatMap.mapSpec.stmts);
//        List<Entry<SparqlStmt, Provenance>> workloads = scriptProcessor.getSparqlStmts();
//
//        List<SparqlStmt> stmts = workloads.stream().map(Entry::getKey).collect(Collectors.toList());
//
//        OutputMode outputMode = OutputModes.detectOutputMode(stmts);
//
//        // This is the final output sink
//        SPARQLResultExProcessor resultProcessor = SPARQLResultExProcessorBuilder.configureProcessor(
//                StdIo.openStdOutWithCloseShield(), System.err,
//                cmdFlatMap.outFormat,
//                stmts,
//                pm,
//                RDFFormat.TURTLE_BLOCKS,
//                RDFFormat.TRIG_BLOCKS,
//                20,
//                false, 0, false,
//                () -> {});
//
//        Function<RDFConnection, SPARQLResultEx> mapper = SparqlMappers.createMapperToSparqlResultEx(outputMode, stmts, resultProcessor);
//
//        Flowable<SPARQLResultEx> flow =
//                // Create a stream of Datasets
//        		NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdFlatMap.nonOptionArgs, null, pm, quadLangs)
//                    // Map the datasets in parallel
//                    .compose(RxOps.createParallelMapperOrdered(
//                        // Map the dataset to a connection
//                        SparqlMappers.mapDatasetToConnection(
//                                // Set context attributes on the connection, e.g. timeouts
//                                SparqlMappers.applyContextHandler(contextHandler)
//                                    // Finally invoke the mapper
//                                    .andThen(mapper))));
//
//        resultProcessor.start();
//        try {
////            for(SPARQLResultEx item : flow.blockingIterable(16)) {
////                System.out.println(item);
////                resultProcessor.forwardEx(item);
////            }
//            RxUtils.consume(flow.map(item -> { resultProcessor.forwardEx(item); return item; }));
//            resultProcessor.finish();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            resultProcessor.flush();
//            resultProcessor.close();
//        }
    }



    public static int merge(CmdNgsMerge cmdMerge) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
//        return 0;
    }


    public static int probe(CmdNgsProbe cmdProbe) throws IOException {
        List<String> args = NamedGraphStreamCliUtils.preprocessArgs(cmdProbe.nonOptionArgs);
        for (String arg : args) {
            // Do not output the arg if there is less-than-or-equal 1
            String prefix = args.size() <= 1 ? "" :  arg + ": ";

            try(TypedInputStream tin = RDFDataMgrEx.open(arg, RDFLanguagesEx.getQuadAndTripleLangs())) {

                String r = tin.getContentType();
                System.out.println(prefix + "[ OK ] " + r);
            } catch(Exception e) {
                String msg = ExceptionUtils.getRootCauseMessage(e);
                System.out.println(prefix + "[FAIL] " + msg);
            }
        }
        return 0;
    }



    public static int sort(CmdNgsSort cmdSort) throws Exception {
        RDFFormat fmt = RDFFormat.TRIG_BLOCKS;

        SparqlQueryParser keyQueryParser = SparqlQueryParserWrapperSelectShortForm.wrap(
                SparqlQueryParserImpl.create(MainCliNamedGraphStream.pm));

        SysSort sysSort = CmdNgsSort.toSysSort(cmdSort);

        FlowableTransformer<Dataset, Dataset> sorter = org.aksw.jena_sparql_api.rx.dataset.DatasetFlowOps.createSystemSorter(sysSort, keyQueryParser);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdSort.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs)
                .compose(sorter);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), fmt);

//		List<String> noas = cmdSort.nonOptionArgs;
//		if(noas.size() != 1) {
//			throw new RuntimeException("Only one non-option argument expected for the artifact id");
//		}
//		String pattern = noas.get(0);
        return 0;
    }

    public static int subjects(CmdNgsSubjects cmdSubjects) throws Exception {
        return groupTriplesByComponent(cmdSubjects, Triple::getSubject);
    }

    /**
     * Common routine for grouping triples by component
     *
     * TODO Clean up
     *
     * @param cmdSubjects
     * @param getFieldValue
     * @return
     * @throws Exception
     */
    public static int groupTriplesByComponent(CmdNgsSubjects cmdSubjects, Function<? super Triple, ? extends Node> getFieldValue) throws Exception {
        Iterable<Lang> tripleLangs = RDFLanguagesEx.getTripleLangs();
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdSubjects.outFormat);

        List<String> args = NamedGraphStreamCliUtils.preprocessArgs(cmdSubjects.nonOptionArgs);
        for(String arg : args) {

            TypedInputStream tmp = RDFDataMgrEx.open(arg, tripleLangs);
            MainCliNamedGraphStream.logger.info("Detected format: " + tmp.getContentType());

            Flowable<Dataset> flow = RDFDataMgrRx.createFlowableTriples(() -> tmp)
                    .compose(NamedGraphStreamOps.groupConsecutiveTriplesByComponent(getFieldValue, DatasetFactoryEx::createInsertOrderPreservingDataset));

            RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);
        }

        return 0;
    }


    public static int until(CmdNgsUntil cmdUntil) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdUntil.outFormat);

        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.get());
        SparqlStmt stmt = stmtParser.apply(cmdUntil.sparqlCondition);
        Query query = stmt.getQuery();

        Predicate<Dataset> condition = MainCliNamedGraphStream.createPredicate(query);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdUntil.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs)
                .takeUntil(condition::test);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);
        return 0;
    }

    public static int wc(CmdNgsWc cmdWc) throws IOException {

        List<String> args = NamedGraphStreamCliUtils.preprocessArgs(cmdWc.nonOptionArgs);

        long totalCount = 0;
        for(String arg : args) {
            String suffix = args.size() <= 1 ? "" : " " +  arg;

            Long count;
            if(cmdWc.numQuads) {
                TypedInputStream tmp = RDFDataMgrEx.open(arg, RDFLanguagesEx.getQuadLangs());
                logger.info("Detected: " + tmp.getContentType() + " on argument " + arg);

                if(cmdWc.noValidate && tmp.getMediaType().equals(Lang.NQUADS.getContentType())) {
                    logger.info("Validation disabled. Resorting to plain line counting");
                    try(BufferedReader br = new BufferedReader(new InputStreamReader(tmp.getInputStream()))) {
                        count = br.lines().count();
                    }
                } else {
                    Lang lang = RDFLanguages.contentTypeToLang(tmp.getContentType());
                    count =  RDFDataMgrRx.createFlowableQuads(() -> tmp, lang, null)
                            .count()
                            .blockingGet();
                }

            } else {
                count = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(Collections.singletonList(arg), null, MainCliNamedGraphStream.pm, quadLangs)
                    .count()
                    .blockingGet();
            }

            totalCount += count;
            String outStr = Long.toString(count) + suffix;
            System.out.println(outStr);
        }

        System.out.println("Counted " + totalCount + " items in total");

        return 0;
    }


    /**
     * Implementation of the the ngs while command.
     * 'while' is a reserved keyword in java hence the 'x' prefix.
     *
     *
     *
     * @param cmdWhile
     * @return
     * @throws Exception
     */
    public static int xwhile(CmdNgsWhile cmdWhile) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdWhile.outFormat);

        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.get());
        SparqlStmt stmt = stmtParser.apply(cmdWhile.sparqlCondition);
        Query query = stmt.getQuery();

        Predicate<Dataset> condition = MainCliNamedGraphStream.createPredicate(query);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdWhile.nonOptionArgs, null, MainCliNamedGraphStream.pm, quadLangs)
                .takeWhile(condition::test);

        RDFDataMgrRx.writeDatasets(flow, StdIo.openStdOutWithCloseShield(), outFormat);
        return 0;
    }

}
