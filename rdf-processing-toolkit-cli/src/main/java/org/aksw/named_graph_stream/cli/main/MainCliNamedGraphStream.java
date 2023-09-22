package org.aksw.named_graph_stream.cli.main;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jenax.stmt.resultset.SPARQLResultSink;
import org.aksw.jenax.stmt.resultset.SPARQLResultSinkQuads;
import org.aksw.named_graph_stream.cli.cmd.CmdNgsMain;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdUtils;
import com.google.common.collect.Streams;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainCliNamedGraphStream {

    static { CliUtils.configureGlobalSettings(); }

//    public static Collection<Lang> quadLangs = Arrays.asList(Lang.TRIG, Lang.NQUADS);
//    public static Collection<Lang> tripleLangs = Arrays.asList(Lang.TURTLE, JenaPluginHdt.LANG_HDT);
//
//    public static Collection<Lang> quadAndTripleLangs = Stream.concat(quadLangs.stream(), tripleLangs.stream())
//            .collect(Collectors.toList());

//    @Deprecated
//    public static final OutputStream out = new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.out));

//    public static OutputStream openStdout() {
//        return new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.out));
//    }


//    public static final OutputStream out = new FileOutputStream(FileDescriptor.out);
    public static final PrefixMapping pm = new PrefixMappingImpl();

    static {
        pm.setNsPrefixes(DefaultPrefixes.get());
        JenaExtensionUtil.addPrefixes(pm);
        JenaExtensionHttp.addPrefixes(pm);
    }

    //public static Collection<Lang> tripleLangs = Arrays.asList(Lang.TURTLE, Lang.NTRIPLES, Lang.RDFXML);

    static final Logger logger = LoggerFactory.getLogger(MainCliNamedGraphStream.class);

    public static void main(String[] args) {
        CmdUtils.execCmd(CmdNgsMain.class, args);
    }

    public static Predicate<Dataset> createPredicate(Query query) {
        return dataset -> {
            boolean result;
            // TODO Allow select / construct forms
            try(QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
                result = qe.execAsk();
            }
            return result;
        };
    }

    public static Function<Dataset, Dataset> createMapper(
            BiConsumer<RDFConnection, SPARQLResultSink> processor) {

        //Sink<Quad> quadSink = SparqlStmtUtils.createSink(RDFFormat.TURTLE_PRETTY, System.err, pm);
        Function<Dataset, Dataset> result = inDs -> {

//				System.out.println("Sleeping thread " + Thread.currentThread());
//				try { Thread.sleep(500); } catch(InterruptedException e) { }

            Dataset out = DatasetFactory.create();

            List<String> names = Streams.stream(inDs.listNames()).collect(Collectors.toList());
            if(names.size() != 1) {
                logger.warn("Expected a single named graph, got " + names);
                return out;
            }
            String name = names.get(0);

            SPARQLResultSinkQuads sink = new SPARQLResultSinkQuads(out.asDatasetGraph()::add);
            try(RDFConnection conn = RDFConnectionFactory.connect(inDs)) {
                processor.accept(conn, sink);
            }

            // The input is guaranteed to be only a single named graph

            // Old approach:
            // If any data was generated in the out's default graph,
            // transfer it to a graph with the input name
            // Issue with old approach: We can not map quads back to triples that easiliy
            boolean defaultGraphToInputGraph = false;
            if(defaultGraphToInputGraph) {
                Model defaultModel = out.getDefaultModel();
                if(!defaultModel.isEmpty()) {
                    Model copy = ModelFactory.createDefaultModel();
                    copy.add(defaultModel);
                    defaultModel.removeAll();
                    //out.setDefaultModel(ModelFactory.createDefaultModel());
                    out.addNamedModel(name, copy);
                }
            }

            return out;
        };

        return result;
    }


}



// TODO We should add a context / function-env attribute
/*
public static BiConsumer<RDFConnection, SPARQLResultSink> createProcessor(
        Collection<SparqlStmt> stmts, Consumer<Context> contextHandler) {
    return (rawConn, sink) -> {
        RDFConnection conn = RDFConnectionFactoryEx.wrapWithContext(rawConn, contextHandler);

        SparqlStmtProcessor stmtProcessor = new SparqlStmtProcessor();

        for(SparqlStmt stmt : stmts) {
            // Some SPARQL query features are not thread safe - clone them!
            SparqlStmt cloneStmt = stmt.clone();
            stmtProcessor.processSparqlStmt(conn, cloneStmt, sink);
        }

    };
}
*/


//public static Function<Dataset, Dataset> createMapper2(Collection<SparqlStmt> stmts) {
//	BiConsumer<RDFConnection, SPARQLResultSink> processor = createProcessor(stmts);
//	Function<Dataset, Dataset> result = createMapper(processor);
//
//	return result;
//}

//flow
//	.map(ds -> Maps.immutableEntry(keyMapper.apply(ds), ds))
//	.map(e -> serialize(e.getKey(), e.getValue()))
//	.compose(composer)
//
//
//		Subscriber<T> tmp = wrap(initiallyExpectedId, incrementSeqId, extractSeqId, e);
//		upstream.subscribe(tmp::onNext, tmp::onError, tmp::onComplete);
//	}
//}, BackpressureStrategy.BUFFER);
//

//public static Function<Dataset, Node> createKeyMapper(Query keyQuery) {
//Function<Dataset, Node> keyMapper;
//
//boolean keyDiffersFromGraph = keyArg != null && !keyArg.isEmpty();
//if(keyDiffersFromGraph) {
//	Query rawKeyQuery = keyQueryParser.apply(keyArg);
//	QueryUtils.optimizePrefixes(rawKeyQuery);
//
//	Query keyQuery = QueryUtils.applyOpTransform(rawKeyQuery, Algebra::unionDefaultGraph);
//
//
//	List<Var> projectVars = rawKeyQuery.getProjectVars();
//	if(projectVars.size() != 1) {
//		throw new RuntimeException("Key query must have exactly 1 result var");
//	}
//	Var keyVar = projectVars.get(0);
//
//	keyMapper = ds -> {
//		QueryExecution qe = QueryExecutionFactory.create(keyQuery, ds);
//		List<Node> nodes = ServiceUtils.fetchList(qe, keyVar);
//
//		Node r = Iterables.getFirst(nodes, NodeFactory.createLiteral(""));
//		return r;
//	};
//} else {
//	keyMapper = ds -> {
//		Iterator<Node> graphNames = ds.asDatasetGraph().listGraphNodes();
//		Node r = Iterators.getNext(graphNames, NodeFactory.createLiteral(""));
//		//Node r = NodeFactory.createURI(rn);
//		return r;
//	};
//}
//
//}


//public static void main2(String[] args) {
//    String raw = "This is	a test \nYay";
//    System.out.println(raw);
//    System.out.println(StringEscapeUtils.escapeJava(raw));
//
//    Random rand = new Random(0);
//    List<String> strs = IntStream.range(0, 1000000)
//        .mapToObj(i -> rand.nextInt(100) + "\t" + RandomStringUtils.randomAlphabetic(10))
//        .collect(Collectors.toList());
//
//    System.out.println("Got random strings");
//    Flowable.fromIterable(strs)
//        .compose(FlowableOps.sysCall(Arrays.asList("/usr/bin/sort", "-h", "-t", "\t")))
//        .timeout(60, TimeUnit.SECONDS)
//        .blockingForEach(System.out::println);
//
//      //.count()
//    //.blockingGet();
//
////	System.out.println(x);
//}
//
//public static void mainCoreOld(String[] args) throws Exception {
//
//
//    CmdNgsMain cmdMain = new CmdNgsMain();
//    CmdNgsSort cmdSort = new CmdNgsSort();
//    CmdNgsHead cmdHead = new CmdNgsHead();
//    CmdNgsFilter cmdFilter = new CmdNgsFilter();
//    CmdNgsWhile cmdWhile = new CmdNgsWhile();
//    CmdNgsUntil cmdUntil = new CmdNgsUntil();
//    CmdNgsCat cmdCat = new CmdNgsCat();
//    CmdNgsMap cmdMap = new CmdNgsMap();
//    CmdNgsWc cmdWc = new CmdNgsWc();
//    CmdNgsProbe cmdProbe = new CmdNgsProbe();
//    CmdNgsSubjects cmdSubjects = new CmdNgsSubjects();
//    CmdNgsMerge cmdMerge = new CmdNgsMerge();
//
//    //CmdNgsConflate cmdConflate = new CmdNgsConflate();
//
//
//    // CommandCommit commit = new CommandCommit();
//    JCommander jc = JCommander.newBuilder()
//            .addObject(cmdMain)
//            .addCommand("subjects", cmdSubjects)
//            .addCommand("sort", cmdSort)
//            .addCommand("head", cmdHead)
//            .addCommand("until", cmdUntil)
//            .addCommand("while", cmdWhile)
//            .addCommand("filter", cmdFilter)
//            .addCommand("cat", cmdCat)
//            .addCommand("map", cmdMap)
//            .addCommand("merge", cmdMerge)
//            .addCommand("wc", cmdWc)
//            .addCommand("probe", cmdProbe)
//
//            .build();
//
//    jc.parse(args);
//    String cmd = jc.getParsedCommand();
//
//
//    if (cmdMain.help || cmd == null) {
//        jc.usage();
//        return;
//    }
//
////    if(true) {
////    	System.out.println(cmdMain.format);
////    	return;
////    }
//
////    OutputStream out = new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.out));
//
//    switch (cmd) {
//    case "subjects": {
//        List<Lang> tripleLangs = RDFLanguagesEx.getTripleLangs();
//        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdSubjects.outFormat);
//
//
//        TypedInputStream tmp = NamedGraphStreamCliUtils.open(cmdSubjects.nonOptionArgs, tripleLangs);
//        MainCliNamedGraphStream.logger.info("Detected format: " + tmp.getContentType());
//
//        Flowable<Dataset> flow = RDFDataMgrRx.createFlowableTriples(() -> tmp)
//                .compose(NamedGraphStreamOps.groupConsecutiveTriplesByComponent(Triple::getSubject, DatasetFactoryEx::createInsertOrderPreservingDataset));
//
//        RDFDataMgrRx.writeDatasets(flow, out, outFormat);
//        break;
//    }
//    case "probe": {
//        try(TypedInputStream tin = NamedGraphStreamCliUtils.open(cmdProbe.nonOptionArgs, quadLangs)) {
////			Collection<Lang> quadLangs = RDFLanguages.getRegisteredLanguages()
////					.stream().filter(RDFLanguages::isQuads)
////					.collect(Collectors.toList());
//
//            String r = tin.getContentType();
//            System.out.println(r);
//        }
//        break;
//    }
//
//    case "wc": {
//        Long count;
//
//        if(cmdWc.numQuads) {
//            TypedInputStream tmp = NamedGraphStreamCliUtils.open(cmdWc.nonOptionArgs, quadLangs);
//            logger.info("Detected: " + tmp.getContentType());
//
//            if(cmdWc.noValidate && tmp.getMediaType().equals(Lang.NQUADS.getContentType())) {
//                logger.info("Validation disabled. Resorting to plain line counting");
//                try(BufferedReader br = new BufferedReader(new InputStreamReader(tmp.getInputStream()))) {
//                    count = br.lines().count();
//                }
//            } else {
//                Lang lang = RDFLanguages.contentTypeToLang(tmp.getContentType());
//                count =  RDFDataMgrRx.createFlowableQuads(() -> tmp, lang, null)
//                        .count()
//                        .blockingGet();
//            }
//
//        } else {
//            count = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdWc.nonOptionArgs, null, pm)
//                .count()
//                .blockingGet();
//        }
//
//        String file = Iterables.getFirst(cmdWc.nonOptionArgs, null);
//        String outStr = Long.toString(count) + (file != null ? " " + file : "");
//        System.out.println(outStr);
//        break;
//    }
//    case "cat": {
//        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdCat.outFormat);
//
//        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdCat.nonOptionArgs, null, pm);
//
//        RDFDataMgrRx.writeDatasets(flow, out, outFormat);
//        break;
//    }
//    case "head": {
//        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdHead.outFormat);
//
//        // parse the numRecord option
//        if(cmdHead.numRecords < 0) {
//            throw new RuntimeException("Negative values not yet supported");
//        }
//
//        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdHead.nonOptionArgs, null, pm)
//            .take(cmdHead.numRecords);
//
//        RDFDataMgrRx.writeDatasets(flow, out, outFormat);
//
//        break;
//    }
//    case "filter": {
//        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdFilter.outFormat);
//
//        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.prefixes);
//        SparqlStmt stmt = stmtParser.apply(cmdFilter.sparqlCondition);
//        Query query = stmt.getQuery();
//
//        Predicate<Dataset> tmpCondition = createPredicate(query);
//
//        Predicate<Dataset> condition = cmdFilter.drop ? tmpCondition.negate() : tmpCondition;
//
//        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdFilter.nonOptionArgs, null, pm)
//                .filter(condition::test);
//
//        RDFDataMgrRx.writeDatasets(flow, out, outFormat);
//        break;
//    }
//    case "while": {
//        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdWhile.outFormat);
//
//        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.prefixes);
//        SparqlStmt stmt = stmtParser.apply(cmdWhile.sparqlCondition);
//        Query query = stmt.getQuery();
//
//        Predicate<Dataset> condition = createPredicate(query);
//
//        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdWhile.nonOptionArgs, null, pm)
//                .takeWhile(condition::test);
//
//        RDFDataMgrRx.writeDatasets(flow, out, outFormat);
//        break;
//    }
//    case "until": {
//        RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdUntil.outFormat);
//
//        Function<String, SparqlStmt> stmtParser = SparqlStmtParserImpl.create(DefaultPrefixes.prefixes);
//        SparqlStmt stmt = stmtParser.apply(cmdUntil.sparqlCondition);
//        Query query = stmt.getQuery();
//
//        Predicate<Dataset> condition = createPredicate(query);
//
//        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdUntil.nonOptionArgs, null, pm)
//                .takeUntil(condition::test);
//
//        RDFDataMgrRx.writeDatasets(flow, out, outFormat);
//        break;
//    }
//    case "merge": {
//        // Create quad streams from all input sources
//
//
//        break;
//    }
//    case "map": {
//        NamedGraphStreamOps.map(pm, cmdMap, out);
//        break;
//    }
//    case "sort": {
//
//        RDFFormat fmt = RDFFormat.TRIG_PRETTY;
//
//        SparqlQueryParser keyQueryParser = SparqlQueryParserWrapperSelectShortForm.wrap(
//                SparqlQueryParserImpl.create(pm));
//
//        FlowableTransformer<Dataset, Dataset> sorter = NamedGraphStreamOps.createSystemSorter(cmdSort, keyQueryParser);
//
//        Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdSort.nonOptionArgs, null, pm)
//                .compose(sorter);
//
//        RDFDataMgrRx.writeDatasets(flow, out, fmt);
//
////		List<String> noas = cmdSort.nonOptionArgs;
////		if(noas.size() != 1) {
////			throw new RuntimeException("Only one non-option argument expected for the artifact id");
////		}
////		String pattern = noas.get(0);
//
//        break;
//    }
//    }
//
////	JCommander deploySubCommands = jc.getCommands().get("sort");
////
////	CommandDeployCkan cmDeployCkan = new CommandDeployCkan();
////	deploySubCommands.addCommand("ckan", cmDeployCkan);
//}
