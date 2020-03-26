package org.aksw.sparql_integrate.ngs.cli.main;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.core.RDFConnectionFactoryEx;
import org.aksw.jena_sparql_api.rx.FlowableTransformerLocalOrdering;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSink;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSinkQuads;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.jena_sparql_api.utils.QueryUtils;
import org.aksw.sparql_integrate.cli.MainCliSparqlStream;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgMain;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsCat;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsHead;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsMap;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsProbe;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSort;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsWc;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;

public class MainCliNamedGraphStream {

    public static Collection<Lang> quadLangs = Arrays.asList(Lang.TRIG, Lang.NQUADS);


    static final Logger logger = LoggerFactory.getLogger(MainCliNamedGraphStream.class);

    public static void main2(String[] args) {
        String raw = "This is	a test \nYay";
        System.out.println(raw);
        System.out.println(StringEscapeUtils.escapeJava(raw));

        Random rand = new Random(0);
        List<String> strs = IntStream.range(0, 1000000)
            .mapToObj(i -> rand.nextInt(100) + "\t" + RandomStringUtils.randomAlphabetic(10))
            .collect(Collectors.toList());

        System.out.println("Got random strings");
        Flowable.fromIterable(strs)
            .compose(FlowableOps.sysCall(Arrays.asList("/usr/bin/sort", "-h", "-t", "\t")))
            .timeout(60, TimeUnit.SECONDS)
            .blockingForEach(System.out::println);

          //.count()
        //.blockingGet();

//		System.out.println(x);
    }

    public static void main(String[] args) throws Exception {
        try {
            mainCore(args);
        } catch(Exception e) {
            //String str = ExceptionUtils.getRootCauseMessage(e);
            String str = ExceptionUtils.getStackTrace(e);
            if(str.toLowerCase().contains("broken pipe")) {
                // Silently ignore
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public static void mainCore(String[] args) throws Exception {

        PrefixMapping pm = new PrefixMappingImpl();
        pm.setNsPrefixes(DefaultPrefixes.prefixes);
        JenaExtensionUtil.addPrefixes(pm);
        JenaExtensionHttp.addPrefixes(pm);


        CmdNgMain cmdMain = new CmdNgMain();
        CmdNgsSort cmdSort = new CmdNgsSort();
        CmdNgsHead cmdHead = new CmdNgsHead();
        CmdNgsCat cmdCat = new CmdNgsCat();
        CmdNgsMap cmdMap = new CmdNgsMap();
        CmdNgsWc cmdWc = new CmdNgsWc();
        CmdNgsProbe cmdProbe = new CmdNgsProbe();

        //CmdNgsConflate cmdConflate = new CmdNgsConflate();


        // CommandCommit commit = new CommandCommit();
        JCommander jc = JCommander.newBuilder()
                .addObject(cmdMain)
                .addCommand("sort", cmdSort)
                .addCommand("head", cmdHead)
                .addCommand("cat", cmdCat)
                .addCommand("map", cmdMap)
                .addCommand("wc", cmdWc)
                .addCommand("probe", cmdProbe)

                .build();

        jc.parse(args);
        String cmd = jc.getParsedCommand();


        if (cmdMain.help || cmd == null) {
            jc.usage();
            return;
        }

//        if(true) {
//        	System.out.println(cmdMain.format);
//        	return;
//        }

        OutputStream out = new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.out));

        switch (cmd) {
        case "probe": {
            try(TypedInputStream tin = NamedGraphStreamCliUtils.open(cmdProbe.nonOptionArgs, quadLangs)) {
//				Collection<Lang> quadLangs = RDFLanguages.getRegisteredLanguages()
//						.stream().filter(RDFLanguages::isQuads)
//						.collect(Collectors.toList());

                String r = tin.getContentType();
                System.out.println(r);
            }
            break;
        }

        case "wc": {
            Long count;

            if(cmdWc.numQuads) {
                TypedInputStream tmp = NamedGraphStreamCliUtils.open(cmdWc.nonOptionArgs, quadLangs);
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
                count = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdWc.nonOptionArgs, null, pm)
                    .count()
                    .blockingGet();
            }

            String file = Iterables.getFirst(cmdWc.nonOptionArgs, null);
            String outStr = Long.toString(count) + (file != null ? " " + file : "");
            System.out.println(outStr);
            break;
        }
        case "cat": {
            RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdCat.outFormat);

            Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdCat.nonOptionArgs, null, pm);

            RDFDataMgrRx.writeDatasets(flow, out, outFormat);
            break;
        }
        case "head": {
            RDFFormat outFormat = RDFLanguagesEx.findRdfFormat(cmdHead.outFormat);

            // parse the numRecord option
            if(cmdHead.numRecords < 0) {
                throw new RuntimeException("Negative values not yet supported");
            }

            Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdHead.nonOptionArgs, null, pm)
                .limit(cmdHead.numRecords);

            RDFDataMgrRx.writeDatasets(flow, out, outFormat);

            break;
        }
        case "map": {
            NamedGraphStreamOps.map(pm, cmdMap, out);
            break;
        }
        case "sort": {

            RDFFormat fmt = RDFFormat.TRIG_PRETTY;

            SparqlQueryParser keyQueryParser = SparqlQueryParserWrapperSelectShortForm.wrap(
                    SparqlQueryParserImpl.create(pm));

            FlowableTransformer<Dataset, Dataset> sorter = NamedGraphStreamOps.createSystemSorter(cmdSort, keyQueryParser);

            Flowable<Dataset> flow = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdSort.nonOptionArgs, null, pm)
                    .compose(sorter);

            RDFDataMgrRx.writeDatasets(flow, out, fmt);

//			List<String> noas = cmdSort.nonOptionArgs;
//			if(noas.size() != 1) {
//				throw new RuntimeException("Only one non-option argument expected for the artifact id");
//			}
//			String pattern = noas.get(0);

            break;
        }
        }

//		JCommander deploySubCommands = jc.getCommands().get("sort");
//
//		CommandDeployCkan cmDeployCkan = new CommandDeployCkan();
//		deploySubCommands.addCommand("ckan", cmDeployCkan);
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


//	public static Function<Dataset, Dataset> createMapper2(Collection<SparqlStmt> stmts) {
//		BiConsumer<RDFConnection, SPARQLResultSink> processor = createProcessor(stmts);
//		Function<Dataset, Dataset> result = createMapper(processor);
//
//		return result;
//	}



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
            // If any data was generated in the out's default graph,
            // transfer it to a graph with the input name
            Model defaultModel = out.getDefaultModel();
            if(!defaultModel.isEmpty()) {
                Model copy = ModelFactory.createDefaultModel();
                copy.add(defaultModel);
                defaultModel.removeAll();
                //out.setDefaultModel(ModelFactory.createDefaultModel());
                out.addNamedModel(name, copy);
            }

            return out;
        };

        return result;
    }

    public static <T, X> FlowableTransformer<T, X> createMapper(
            PrefixMapping pm,
            List<String> sparqlSrcs, //CmdNgsMap cmdMap,
            Function<? super T, ? extends Dataset> getDataset,
            BiFunction<? super T, ? super Dataset, X> setDataset,
            Consumer<Context> contextHandler) throws FileNotFoundException, IOException, ParseException {

        BiConsumer<RDFConnection, SPARQLResultSink> coreProcessor =
                MainCliSparqlStream.createProcessor(sparqlSrcs, pm, true);


        // Wrap the core processor with modifiers for theo context
        BiConsumer<RDFConnection, SPARQLResultSink> processor = (coreConn, sink) -> {
            RDFConnection c = contextHandler == null
                ? coreConn
                : RDFConnectionFactoryEx.wrapWithContext(coreConn, contextHandler);

            coreProcessor.accept(c, sink);
        };


        Function<Dataset, Dataset> mapper = createMapper(processor);

        return in -> in
            .zipWith(() -> LongStream.iterate(0, i -> i + 1).iterator(), Maps::immutableEntry)
            .parallel(Runtime.getRuntime().availableProcessors(), 8) // Prefetch only few items
            .runOn(Schedulers.io())
            //.observeOn(Schedulers.computation())
            .map(e -> {
                T item = e.getKey();
                Dataset before = getDataset.apply(item);
                Dataset after = mapper.apply(before);
                X r = setDataset.apply(item, after);
                return Maps.immutableEntry(r, e.getValue());
            })
            // Experiment with performing serialization already in the thread
            // did not show much benefit
    //			.map(e -> {
    //				Dataset tmp = e.getKey();
    //				String str = toString(tmp, RDFFormat.TRIG_PRETTY);
    //				return Maps.immutableEntry(str, e.getValue());
    //			})
            .sequential()
            .compose(FlowableTransformerLocalOrdering.transformer(0l, i -> i + 1, (a, b) -> a - b, Entry::getValue))
            //.sorted((a, b) -> Objects.compare(a.getValue(), b.getValue(), Ordering.natural().re))
            // .sequential()
//            .doAfterNext(item -> System.err.println("GOT AFTER SEQUENTIAL: " + item.getValue() + " in thread " + Thread.currentThread()))
    //			.doAfterNext(System.out::println)
//            .doAfterNext(item -> System.err.println("GOT AFTER LOCAL ORDERING: " + item.getValue() + " in thread " + Thread.currentThread()))
            .map(Entry::getKey);
    }


    public static FlowableTransformer<Dataset, Dataset> createMapper(Consumer<Context> contextHandler, String ... sparqlResources) {
        FlowableTransformer<Dataset, Dataset> result;
        try {
            result = createMapper(DefaultPrefixes.prefixes, Arrays.asList(sparqlResources), ds -> ds, (before, after) -> after, contextHandler);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Flowable<Dataset> mapCore(Consumer<Context> contextHandler, PrefixMapping pm, CmdNgsMap cmdFlatMap)
            throws FileNotFoundException, IOException, ParseException {

        FlowableTransformer<Dataset, Dataset> mapper = createMapper(pm, cmdFlatMap.stmts, ds -> ds, (before, after) -> after, contextHandler);

        Flowable<Dataset> result = NamedGraphStreamCliUtils.createNamedGraphStreamFromArgs(cmdFlatMap.nonOptionArgs, null, pm)
                .compose(mapper);

        return result;
    }



    // public static final UnaryRelation DISTINCT_NAMED_GRAPHS = Concept.create("GRAPH ?g { ?s ?p ?o }", "g");
    public static final Query DISTINCT_NAMED_GRAPHS = QueryFactory.create("SELECT DISTINCT ?g { GRAPH ?g { ?s ?p ?o } }");



    public static Function<? super SparqlQueryConnection, Node> createKeyMapper(
            String keyArg,
            Function<String, Query> queryParser,
            Query fallback) {
        //Function<Dataset, Node> keyMapper;

        Query effectiveKeyQuery;
        boolean useFallback = Strings.isNullOrEmpty(keyArg);
        if(!useFallback) {
            effectiveKeyQuery = queryParser.apply(keyArg);
            QueryUtils.optimizePrefixes(effectiveKeyQuery);
        } else {
            effectiveKeyQuery = fallback;
        }

        Function<? super SparqlQueryConnection, Node> result = ResultSetMappers.createNodeMapper(effectiveKeyQuery, NodeFactory.createLiteral(""));
        return result;
    }
}


//

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


