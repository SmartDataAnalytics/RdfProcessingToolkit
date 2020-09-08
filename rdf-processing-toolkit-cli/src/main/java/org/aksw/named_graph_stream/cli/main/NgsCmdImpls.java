package org.aksw.named_graph_stream.cli.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
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
import org.aksw.sparql_integrate.cli.SparqlScriptProcessor;
import org.aksw.sparql_integrate.cli.SparqlScriptProcessor.Provenance;
import org.aksw.sparql_integrate.cli.main.OutputMode;
import org.aksw.sparql_integrate.cli.main.SPARQLResultExProcessor;
import org.aksw.sparql_integrate.cli.main.SparqlIntegrateCmdImpls;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.TransformUnionQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.http.Service;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import joptsimple.internal.Strings;

/**
 * Implementation of all named graph stream commands as static methods.
 *
 *
 * @author raven
 *
 */
public class NgsCmdImpls {
    // FIXME Clean this up (use spring boot?)
    static { SparqlIntegrateCmdImpls.configureGlobalSettings(); }

    private static final Logger logger = LoggerFactory.getLogger(NgsCmdImpls.class);

    public static int cat(CmdNgsCat cmdCat) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdCat.outFormat);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdCat.nonOptionArgs, null, MainCliNamedGraphStream.pm);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);
        return 0;
    }


    public static int filter(CmdNgsFilter cmdFilter) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdFilter.outFormat);

        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.prefixes);
        SparqlStmt stmt = stmtParser.apply(cmdFilter.sparqlCondition);
        Query query = stmt.getQuery();

        Predicate<Dataset> tmpCondition = MainCliNamedGraphStream.createPredicate(query);

        Predicate<Dataset> condition = cmdFilter.drop ? tmpCondition.negate() : tmpCondition;

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdFilter.nonOptionArgs, null, MainCliNamedGraphStream.pm)
                .filter(condition::test);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);
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


        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdHead.nonOptionArgs, null, MainCliNamedGraphStream.pm)
            .take(val);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);

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

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdTail.nonOptionArgs, null, MainCliNamedGraphStream.pm)
            .skip(val);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);

        return 0;
    }

    /**
     * Quad-based mapping (rather than Dataset-based).
     *
     * @param cmdMap
     */
    public static int mapQuads(CmdNgsMap cmdMap) throws Exception {
        Iterable<Lang> probeLangs = MainCliNamedGraphStream.quadAndTripleLangs;

        List<String> args = preprocessArgs(cmdMap.nonOptionArgs);
        Map<String, Callable<TypedInputStream>> map = validate(args, probeLangs, true);

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

        RDFDataMgrRx.writeQuads(quadFlow, MainCliNamedGraphStream.out, RDFFormat.NQUADS);
            //.forEach(q -> RDFDataMgr.writeQuads(MainCliNamedGraphStream.out, Collections.singleton(q).iterator()));
//            .forEach(q -> NQuadsWriter.write(MainCliNamedGraphStream.out, Collections.singleton(q).iterator()));

        return 0;
    }


    public static int map(CmdNgsMap cmdMap) throws Exception {
        if (cmdMap.mapSpec.defaultGraph) {
            cmdMap.mapSpec.graph = Quad.defaultGraphIRI.toString();
        }

        if (cmdMap.mapSpec.graph != null) {
            mapQuads(cmdMap);
        } else {
            // NamedGraphStreamOps.map(MainCliNamedGraphStream.pm, cmdMap, MainCliNamedGraphStream.out);
            execMap(MainCliNamedGraphStream.pm, cmdMap);
        }

        return 0;
    }



    public static void execMap(PrefixMapping pm, CmdNgsMap cmdFlatMap) {

        String timeoutSpec = cmdFlatMap.serviceTimeout;
        Consumer<Context> contextHandler = cxt -> {
            if (!Strings.isNullOrEmpty(timeoutSpec)) {
                cxt.set(Service.queryTimeout, timeoutSpec);
            }
        };

        SparqlScriptProcessor scriptProcessor = SparqlScriptProcessor.create(pm);

        // Register a (best-effort) union default graph transform
        scriptProcessor.addPostTransformer(stmt -> SparqlStmtUtils.applyOpTransform(stmt,
                op -> Transformer.transformSkipService(new TransformUnionQuery(), op)));


        scriptProcessor.process(cmdFlatMap.mapSpec.stmts);
        List<Entry<SparqlStmt, Provenance>> workloads = scriptProcessor.getSparqlStmts();

        List<SparqlStmt> stmts = workloads.stream().map(Entry::getKey).collect(Collectors.toList());

        OutputMode outputMode = SparqlIntegrateCmdImpls.detectOutputMode(stmts);

        // This is the final output sink
        SPARQLResultExProcessor resultProcessor = SparqlIntegrateCmdImpls.configureProcessor(
                cmdFlatMap.outFormat,
                stmts,
                pm,
                false,
                0,
                false);

        Function<RDFConnection, SPARQLResultEx> mapper = SparqlMappers.createMapperFromDataset(outputMode, stmts, resultProcessor);

        Flowable<SPARQLResultEx> flow =
                // Create a stream of Datasets
                NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdFlatMap.nonOptionArgs, null, pm)
                    // Map the datasets in parallel
                    .compose(SparqlMappers.createParallelMapperOrdered(
                        // Map the dataset to a connection
                        SparqlMappers.datasetAsConnection(
                                // Set context attributes on the connection, e.g. timeouts
                                SparqlMappers.applyContextHandler(contextHandler)
                                    // Finally invoke the mapper
                                    .andThen(mapper))));

        resultProcessor.start();
        flow.blockingForEach(item -> resultProcessor.forwardEx(item));
        resultProcessor.finish();
        resultProcessor.flush();
        resultProcessor.close();
    }


    public static int merge(CmdNgsMerge cmdMerge) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
//        return 0;
    }


    public static int probe(CmdNgsProbe cmdProbe) throws IOException {
        List<String> args = preprocessArgs(cmdProbe.nonOptionArgs);
        for (String arg : args) {
            // Do not output the arg if there is less-than-or-equal 1
            String prefix = args.size() <= 1 ? "" :  arg + ": ";

            try(TypedInputStream tin = RDFDataMgrEx.open(arg, MainCliNamedGraphStream.quadAndTripleLangs)) {

                String r = tin.getContentType();
                System.out.println(prefix + "[ OK ] " + r);
            } catch(Exception e) {
                String msg = ExceptionUtils.getRootCauseMessage(e);
                System.out.println(prefix + "[FAIL] " + msg);
            }
        }
        return 0;
    }

    /**
     * Injects stdin if there are no arguments and checks that stdin is not mixed with
     * outher input sources
     *
     * @param args
     * @return
     */
    public static List<String> preprocessArgs(List<String> args) {
        List<String> result = args.isEmpty() ? Collections.singletonList("-") : args;

        validateStdIn(args);

        return result;
    }


    /**
     *  If one of the args is '-' for STDIN there must not be any further arg
     *
     * @param args
     */
    public static void validateStdIn(List<String> args) {
        long stdInCount = args.stream().filter(item -> item.equals("-")).count();
        if (stdInCount != 0 && args.size() > 1) {
            throw new RuntimeException("If STDIN (denoted by '-') is used no further input sources may be used");
        }
    }

    public static Callable<TypedInputStream> validate(String filenameOrIri, Iterable<Lang> probeLangs, boolean displayProbeResult) {
        Callable<TypedInputStream> result;
        if (RDFDataMgrEx.isStdIn(filenameOrIri)) {
            TypedInputStream tin = RDFDataMgrEx.forceBuffered(RDFDataMgrEx.open(filenameOrIri, probeLangs));

            // Beware that each invocation of the supplier returns the same underlying input stream
            // however with a fresh close shield! The purpose is to allow probing on stdin
            result = () -> RDFDataMgrEx.wrapInputStream(new CloseShieldInputStream(tin.getInputStream()), tin);
        } else {
            try(TypedInputStream tin = RDFDataMgrEx.open(filenameOrIri, probeLangs)) {
                String ct = tin.getContentType();
                Lang lang = RDFLanguages.contentTypeToLang(ct);
                if (displayProbeResult) {
                    MainCliNamedGraphStream.logger.info("Detected format: " + filenameOrIri + " " + ct);
                }

                result = () -> RDFDataMgrEx.forceBuffered(RDFDataMgrEx.open(filenameOrIri, Arrays.asList(lang)));
            }
        }

        return result;
    }

    /**
     * Validate whether all given arguments can be opened.
     * This is similar to probe() except that an exception is raised on error
     *
     * @param args
     * @param probeLangs
     */
    public static Map<String, Callable<TypedInputStream>> validate(List<String> args, Iterable<Lang> probeLangs, boolean displayProbeResults) {

        Map<String, Callable<TypedInputStream>> result = new LinkedHashMap<>();

        validateStdIn(args);

        int violationCount = 0;
        for (String arg : args) {

            try {
                Callable<TypedInputStream> inSupp = validate(arg, probeLangs, displayProbeResults);
                result.put(arg, inSupp);
            } catch(Exception e) {
                String msg = ExceptionUtils.getRootCauseMessage(e);
                MainCliNamedGraphStream.logger.info(arg + ": " + msg);

                ++violationCount;
            }
        }

        if (violationCount != 0) {
            throw new IllegalArgumentException("Some arguments failed to validate");
        }

        return result;
    }


    public static int sort(CmdNgsSort cmdSort) throws Exception {
        RDFFormat fmt = RDFFormat.TRIG_PRETTY;

        SparqlQueryParser keyQueryParser = SparqlQueryParserWrapperSelectShortForm.wrap(
                SparqlQueryParserImpl.create(MainCliNamedGraphStream.pm));

        FlowableTransformer<Dataset, Dataset> sorter = NamedGraphStreamOps.createSystemSorter(cmdSort, keyQueryParser);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdSort.nonOptionArgs, null, MainCliNamedGraphStream.pm)
                .compose(sorter);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, fmt);

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
        Iterable<Lang> tripleLangs = MainCliNamedGraphStream.tripleLangs;
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdSubjects.outFormat);

        List<String> args = preprocessArgs(cmdSubjects.nonOptionArgs);
        for(String arg : args) {

            TypedInputStream tmp = RDFDataMgrEx.open(arg, tripleLangs);
            MainCliNamedGraphStream.logger.info("Detected format: " + tmp.getContentType());

            Flowable<Dataset> flow = RDFDataMgrRx.createFlowableTriples(() -> tmp)
                    .compose(NamedGraphStreamOps.groupConsecutiveTriplesByComponent(getFieldValue, DatasetFactoryEx::createInsertOrderPreservingDataset));

            RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);
        }

        return 0;
    }


    public static int until(CmdNgsUntil cmdUntil) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdUntil.outFormat);

        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.prefixes);
        SparqlStmt stmt = stmtParser.apply(cmdUntil.sparqlCondition);
        Query query = stmt.getQuery();

        Predicate<Dataset> condition = MainCliNamedGraphStream.createPredicate(query);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdUntil.nonOptionArgs, null, MainCliNamedGraphStream.pm)
                .takeUntil(condition::test);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);
        return 0;
    }

    public static int wc(CmdNgsWc cmdWc) throws IOException {

        List<String> args = preprocessArgs(cmdWc.nonOptionArgs);

        for(String arg : args) {
            String suffix = args.size() <= 1 ? "" : " " +  arg;

            Long count;
            if(cmdWc.numQuads) {
                TypedInputStream tmp = RDFDataMgrEx.open(arg, MainCliNamedGraphStream.quadLangs);
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
                count = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdWc.nonOptionArgs, null, MainCliNamedGraphStream.pm)
                    .count()
                    .blockingGet();
            }

            String outStr = Long.toString(count) + suffix;
            System.out.println(outStr);
        }
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

        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.prefixes);
        SparqlStmt stmt = stmtParser.apply(cmdWhile.sparqlCondition);
        Query query = stmt.getQuery();

        Predicate<Dataset> condition = MainCliNamedGraphStream.createPredicate(query);

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdWhile.nonOptionArgs, null, MainCliNamedGraphStream.pm)
                .takeWhile(condition::test);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);
        return 0;
    }

}
