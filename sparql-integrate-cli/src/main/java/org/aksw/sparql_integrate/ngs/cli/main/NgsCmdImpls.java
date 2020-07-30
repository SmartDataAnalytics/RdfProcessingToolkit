package org.aksw.sparql_integrate.ngs.cli.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsCat;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsFilter;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsHead;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsMap;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsMerge;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsProbe;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSort;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSubjects;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsUntil;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsWc;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsWhile;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
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


    public static int map(CmdNgsMap cmdMap) throws Exception {
        NamedGraphStreamOps.map(MainCliNamedGraphStream.pm, cmdMap, MainCliNamedGraphStream.out);
        return 0;
    }


    public static int merge(CmdNgsMerge cmdMerge) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
//        return 0;
    }


    public static int probe(CmdNgsProbe cmdProbe) throws IOException {
        try(TypedInputStream tin = NamedGraphStreamCliUtils.open(cmdProbe.nonOptionArgs, MainCliNamedGraphStream.quadLangs)) {
//			Collection<Lang> quadLangs = RDFLanguages.getRegisteredLanguages()
//					.stream().filter(RDFLanguages::isQuads)
//					.collect(Collectors.toList());

            String r = tin.getContentType();
            System.out.println(r);
        }
        return 0;
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
        List<Lang> tripleLangs = RDFLanguagesEx.getTripleLangs();
        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdSubjects.outFormat);


        TypedInputStream tmp = NamedGraphStreamCliUtils.open(cmdSubjects.nonOptionArgs, tripleLangs);
        MainCliNamedGraphStream.logger.info("Detected format: " + tmp.getContentType());

        Flowable<Dataset> flow = RDFDataMgrRx.createFlowableTriples(() -> tmp)
                .compose(NamedGraphStreamOps.groupConsecutiveTriplesByComponent(Triple::getSubject, DatasetFactoryEx::createInsertOrderPreservingDataset));

        RDFDataMgrRx.writeDatasets(flow, MainCliNamedGraphStream.out, outFormat);
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
        Long count;

        if(cmdWc.numQuads) {
            TypedInputStream tmp = NamedGraphStreamCliUtils.open(cmdWc.nonOptionArgs, MainCliNamedGraphStream.quadLangs);
            logger.info("Detected: " + tmp.getContentType());

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

        String file = Iterables.getFirst(cmdWc.nonOptionArgs, null);
        String outStr = Long.toString(count) + (file != null ? " " + file : "");
        System.out.println(outStr);
        return 0;
    }


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
