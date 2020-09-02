package org.aksw.ngs.cli.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.ngs.cli.cmd.CmdNgsCat;
import org.aksw.ngs.cli.cmd.CmdNgsFilter;
import org.aksw.ngs.cli.cmd.CmdNgsHead;
import org.aksw.ngs.cli.cmd.CmdNgsMap;
import org.aksw.ngs.cli.cmd.CmdNgsMerge;
import org.aksw.ngs.cli.cmd.CmdNgsProbe;
import org.aksw.ngs.cli.cmd.CmdNgsSort;
import org.aksw.ngs.cli.cmd.CmdNgsSubjects;
import org.aksw.ngs.cli.cmd.CmdNgsTail;
import org.aksw.ngs.cli.cmd.CmdNgsUntil;
import org.aksw.ngs.cli.cmd.CmdNgsWc;
import org.aksw.ngs.cli.cmd.CmdNgsWhile;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
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
        if(cmdHead.numRecords < 0) {
            throw new RuntimeException("Negative values not yet supported");
        }

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdHead.nonOptionArgs, null, MainCliNamedGraphStream.pm)
            .take(cmdHead.numRecords);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);

        return 0;
    }

    public static int tail(CmdNgsTail cmdTail) throws Exception {
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdTail.outFormat);

        // parse the numRecord option
        if(cmdTail.numRecords < 0) {
            throw new RuntimeException("Negative values not yet supported");
        }

        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdTail.nonOptionArgs, null, MainCliNamedGraphStream.pm)
            .skip(cmdTail.numRecords);

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);

        return 0;
    }



    /**
     * Quad-based mapping (rather than Dataset-based).
     *
     * @param cmdMap
     */
    public static int mapQuads(CmdNgsMap cmdMap) {
        List<String> args = preprocessArgs(cmdMap.nonOptionArgs);
        validate(args, MainCliNamedGraphStream.quadLangs, true);

        String graphIri = cmdMap.mapSpec.graph;
        Node g = NodeFactory.createURI(graphIri);

        Function<Quad, Quad> quadMapper = q -> new Quad(g, q.asTriple());

        Flowable.fromIterable(args)
            .flatMap(arg -> {
                Flowable<Quad> r = RDFDataMgrRx.createFlowableQuads(() ->
                    RDFDataMgrEx.open(arg, MainCliNamedGraphStream.quadLangs))
                .map(quad -> quadMapper.apply(quad));
                return r;
            })
            .forEach(q -> RDFDataMgr.writeQuads(MainCliNamedGraphStream.out, Collections.singleton(q).iterator()));
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
            NamedGraphStreamOps.map(MainCliNamedGraphStream.pm, cmdMap, MainCliNamedGraphStream.out);
        }

        return 0;
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
    /**
     * Validate whether all given arguments can be opened.
     * This is similar to probe() except that an exception is raised on error
     *
     * @param args
     * @param probeLangs
     */
    public static void validate(List<String> args, Iterable<Lang> probeLangs, boolean displayProbeResults) {

        validateStdIn(args);

        int violationCount = 0;
        for (String arg : args) {
            if (!arg.equals("-")) {

                // Do not output the arg if there is less-than-or-equal 1
                String prefix = args.size() <= 1 ? "" :  arg + ": ";

                try(TypedInputStream tin = RDFDataMgrEx.open(arg, probeLangs)) {
                    if (displayProbeResults) {
                        MainCliNamedGraphStream.logger.info("Detected format: " + prefix + " " + tin.getContentType());
                    }
                    // success
                } catch(Exception e) {
                    String msg = ExceptionUtils.getRootCauseMessage(e);
                    System.err.println(prefix + msg);

                    ++violationCount;
                }
            }
        }

        if (violationCount != 0) {
            throw new IllegalArgumentException("Some arguments failed to validate");
        }
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
        List<Lang> tripleLangs = RDFLanguagesEx.getTripleLangs();
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
