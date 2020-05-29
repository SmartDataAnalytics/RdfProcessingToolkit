package org.aksw.data_profiler.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.io.binseach.GraphFromSubjectCache;
import org.aksw.jena_sparql_api.io.lib.SpecialGraphs;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.SparqlRx;
import org.aksw.jena_sparql_api.rx.query_flow.QueryFlowOps;
import org.aksw.jena_sparql_api.rx.query_flow.RxUtils;
import org.aksw.jena_sparql_api.utils.Vars;
import org.apache.jena.ext.com.google.common.cache.CacheBuilder;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.lang.SinkTriplesToGraph;
import org.apache.jena.riot.out.SinkTripleOutput;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VOID;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;



@Command(name = "example", mixinStandardHelpOptions = true, version = "Picocli example 4.0")
public class MainCliVoidGeneratorMinimal
    implements Runnable {

    /**
     * Manually cleaned output: (exclusions: "qf6", "qf7", "qf8" because of union queries)
     * l1    qa1.sparql [qa1.sparql, qa2.sparql, qb1.sparql, qb3.sparql, qb4.sparql, qb5.sparql, qd1.sparql, qd2.sparql, qd3.sparql, qd4.sparql] # ?s ?p ?o
     * l11     qb2.sparql [qb2.sparql, qc1.sparql, qc5.sparql] # FILTER(?p = rdf:type)
     * l111      qc3.sparql [qc3.sparql] # ?s ?p ?d FILTER(?p = rdf:type)
     * l112      qc6.sparql [qc2.sparql, qc4.sparql, qc6.sparql, qe1.sparql, qe2.sparql, qe3.sparql, qe4.sparql, qf9.sparql] # ?s ?y ?z FILTER(?y = rdf:type)
     * l113      qf10.sparql [qf10.sparql] # ?o ?y ?z FILTER(?y = rdf:type)
     * l12     qf1.sparql [qf1.sparql] # FILTER(isIri(?s))
     * l13     qf2.sparql [qf2.sparql] # FILTER(isBlank(?s))
     * l14     qf3.sparql [qf3.sparql] # FILTER(isIri(?o))
     * l15     qf4.sparql [qf4.sparql] # FILTER(isLiteral(?o)
     * l16     qf5.sparql [qf5.sparql] # FILTER(isBlank(?o))
     */

    //@Parameters(arity = "1..*", paramLabel = "FILE", description = "File(s) to process.")
    @Parameters(arity = "1", paramLabel = "FILE", description = "File(s) to process.")
    private Path inputFile;

    @Option(names="--no-star", arity = "0..1", description = "Disable star pattern queries")
    protected boolean noStar = false;

    @Option(names="--no-path", arity = "0..1", description = "Disable path pattern queries")
    protected boolean noPath = false;

    @Option(names="--parallel", arity = "0..1", description = "Number of *worker* processors to use")
    protected int parallel = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);


//    @Option(names="--no-single", arity = "0..1", description = "Disable singl pattern queries")
//    protected boolean noSingle = false;

    public static final Query spoQuery = QueryFactory.create("SELECT * { ?s ?p ?o }");


    @Override
    public void run() {
        Stopwatch sw = Stopwatch.createStarted();

        try {
            doRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.err.println("Done after " + sw.elapsed(TimeUnit.MILLISECONDS) * 0.001 + " seconds");
    }

    public void doRun() throws Exception {
//        System.out.println("Files: " + inputFile);

        String fileName = inputFile.getFileName().toString();



        // Benchmark plain parsing throughput
        if(false) {
            Stopwatch sw = Stopwatch.createStarted();
            int i[] = {0};
            int throughputUpdateInterval = 100000;
            RDFDataMgrRx.createFlowableTriples(inputFile.toAbsolutePath().toString(), Lang.NTRIPLES, null)
                    // .subscribeOn(Schedulers.io())
                    .doOnNext(x -> {
                        //System.out.println(NodeFmtLib.str(x));
                        ++i[0];
                        if(i[0] % throughputUpdateInterval == 0) {
                            System.err.println("Throughput of triples/second since start: " + i[0] / (sw.elapsed(TimeUnit.MILLISECONDS) * 0.001f) + " (update interval = " + throughputUpdateInterval + ")");
                        }
                    })
                    .subscribe();
            return;
        }



        Graph inGraph;
        if(fileName.endsWith(".bz2")) {
            inGraph = SpecialGraphs.fromSortedNtriplesBzip2File(inputFile);
        } else {
            inGraph = SpecialGraphs.fromSortedNtriplesFile(inputFile);
        }

        inGraph = new GraphFromSubjectCache(inGraph, CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(10000)
                .concurrencyLevel(1)
                .build());

        Graph ffs = inGraph;

        boolean copyToMemGraph = false;

        if(copyToMemGraph) {
            Graph tmp = GraphFactory.createDefaultGraph();
            GraphUtil.addInto(tmp, inGraph);
            inGraph = tmp;
        }



        // Benchmark graph find parsing throughput
        if(true) {
            Stopwatch sw = Stopwatch.createStarted();
            int i[] = {0};
            int throughputUpdateInterval = 100000;
            QueryFlowOps.wrapClosableIteratorSupplier(() -> ffs.find())
                    // .subscribeOn(Schedulers.io())
                    .doOnNext(x -> {
                        //System.out.println(NodeFmtLib.str(x));
                        ++i[0];
                        if(i[0] % throughputUpdateInterval == 0) {
                            System.err.println("Throughput of triples/second since start: " + i[0] / (sw.elapsed(TimeUnit.MILLISECONDS) * 0.001f) + " (update interval = " + throughputUpdateInterval + ")");
                        }
                    })
                    .subscribe();
            return;
        }

        boolean testGraphFind = false;
        if(testGraphFind) {
            if(true) {
            ExtendedIterator<Triple> it = inGraph.find();
            while(it.hasNext()) {
                Triple t = it.next();
//                System.out.println(t);
                //Thread.sleep(100);
                ExtendedIterator<Triple> join = inGraph.find(t.getSubject(), RDF.Nodes.type, Node.ANY);
                while(join.hasNext()) {
                    Triple u = join.next();
                    System.out.println(u);
                }

            }
            it.close();
            }


            QueryFlowOps.wrapClosableIteratorSupplier(() -> ffs.find())
            .map(t -> {
                BindingMap r = new BindingHashMap();
                r.add(Vars.s, t.getSubject());
                r.add(Vars.p, t.getPredicate());
                r.add(Vars.o, t.getObject());
                return r;
            })
            .flatMap(QueryFlowOps.createMapperForJoin(ffs, new Triple(Vars.s, Node.ANY, Vars.x))::apply)
            .doOnComplete(() -> System.err.println("Done!"))
            .subscribe();
            //.subscribe(x -> System.out.println(x));



            return;
        }


        boolean debugJoin = false;
        if(debugJoin) {
            Stopwatch sw = Stopwatch.createStarted();
            long cnt = SparqlRx.execSelectRaw(() -> QueryExecutionFactory.create(spoQuery, DatasetGraphFactory.wrap(ffs)))
                    .flatMap(QueryFlowOps.createMapperForJoin(ffs, new Triple(Vars.s, RDF.Nodes.type, Vars.t))::apply)
                    .count()
                    .blockingGet();

            System.out.println("Count: " + cnt + " " + sw.elapsed(TimeUnit.MILLISECONDS) * 0.001);
            System.out.println(((GraphFromSubjectCache)inGraph).getSubjectCache().stats());
            return;
        }

        // Graph outGraph = GraphFactory.createDefaultGraph();
        // SinkTriplesToGraph sink = new SinkTriplesToGraph(false, outGraph);


        SinkTripleOutput sink = new SinkTripleOutput(System.out, null, null);
//        Sink<Triple> sink = new SinkNull<>();


        //Supplier<SinkTripleOutput> sinkSupp = () -> sink;

        //Supplier<MySinkTriplesToGraph> sinkToGraphSupp = () -> new MySinkTriplesToGraph(GraphFactory.createDefaultGraph());

//        RxWorkflow<AccSinkTriples<MySinkTriplesToGraph>> workflow = generateDataProfileForVoid(sinkToGraphSupp, inGraph);
        System.err.println("Worker thread pool size: " + parallel);

        Executor executor = Executors.newFixedThreadPool(parallel);
        Scheduler workerScheduler = Schedulers.newThread();
//        Scheduler workerScheduler = Schedulers.from(executor);
//        Scheduler workerScheduler = Schedulers.computation();

        RxWorkflow<Triple> workflow = generateDataProfileForVoid(inGraph, workerScheduler, !noStar, !noPath);

            //.subscribe(x -> System.err.println("DONE EVENT FIRED"));
        System.err.println("Active tasks: " + workflow.getTasks().keySet());

        // First: Subscribe to the tasks
//        Flowable<Flowable<?>> tasks = Flowable.fromIterable(Iterables.concat(Collections.singleton(workflow.getRootFlowable()), workflow.getTasks().values()));
        //.flatMap(task -> task.observeOn(workerScheduler))
        //.map(x -> System.err.println("DONE EVENT FIRED"))
        //Single<Long> waitForTasks =
//        Flowable<Flowable<Triple>> tasks = Flowable.fromIterable(workflow.getTasks().values());
        //Iterable<Triple> triples = tasks
        //tasks
            //.flatMap(task -> task.observeOn(workerScheduler))
            //.flatMap(task -> task.subscribeOn(workerScheduler))
            //(task -> task.observeOn(workerScheduler).ignoreElements())

//        .count();

//        Flowable<Triple> task = Flowable.fromIterable(workflow.getTasks().values())
//                .flatMap(x -> x);
//                .flatMap(x -> x.observeOn(workerScheduler));
//                .flatMap(x -> x.observeOn(Schedulers.computation()));

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for(Entry<String, Flowable<Triple>> e : workflow.getTasks().entrySet()) {
            String key = e.getKey();
            Flowable<Triple> task = e.getValue();
            CompletableFuture<?> future = new CompletableFuture<>();
            futures.add(future);

            System.err.println("Subscribing to " + key);
            task
//                .observeOn(workerScheduler)
//                .observeOn(Schedulers.computation())
                //.doOnNext(item -> System.err.println("Seen " + key + " on thread " + Thread.currentThread()))
                .subscribe(sink::send, err -> {}, () -> {
                    System.err.println("Resolving future for " + key);
                    //Thread.sleep(1000);
                    future.complete(null);
                });
            System.err.println("Subscription to " + key + " complete");
        }
        CompletableFuture<?> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));

//        CompletableFuture<?> future = new CompletableFuture<>();
//        task.subscribe(sink::send, e -> {}, () -> future.complete(null));

        System.err.println("Subscribed to tasks");

        // Second: Start the root flowable
        System.err.println("Connecting to root flow");
        workflow.getRootFlowable().connect();

        System.err.println("Root flow .connect() returned; waiting for future to resolve");

        future.get();

        // Wait for tasks to complete
//        for(Triple t : triples) {
//            sink.send(t);
//        }

        sink.flush();

//        System.err.println("Finished tasks: " + tasksFinished);
        //.blockingGet()


//        for(Entry<String, AccSinkTriples<MySinkTriplesToGraph>> e : workflow.getSinks().entrySet()) {
//            System.out.println(e.getKey());
//            Graph g = e.getValue().getValue().getGraph();
//            Model mm = ModelFactory.createModelForGraph(g);
//            RDFDataMgr.write(System.out, mm, RDFFormat.TURTLE_PRETTY);
//        }

        sink.close();

        if(inGraph instanceof GraphFromSubjectCache) {
            System.err.println("Cache stats: " + ((GraphFromSubjectCache)inGraph).getSubjectCache().stats());
        }

//        Model m = ModelFactory.createModelForGraph(outGraph);
//        RDFDataMgr.write(System.out, dataset, lang);

    }

    public static ResultSet fromTable(Table table, ExecutionContext execCxt) {
        ResultSet result = ResultSetFactory.create(table.iterator(execCxt), table.getVarNames());
        return result;
    }

    public static void main(String[] args) {
         int exitCode = new CommandLine(new MainCliVoidGeneratorMinimal()).execute(args);
         System.exit(exitCode);
    }


    public static RxWorkflow<Triple> generateDataProfileForVoid(
            Graph graph,
            Scheduler workerScheduler,
            boolean enableStarJoin,
            boolean enablePathJoin) throws Exception {

        int counterInterval = 100000;
        int throughputUpdateInterval = 100000;


        ExecutionContext execCxt = QueryFlowOps.createExecutionContextDefault();

        Map<String, Flowable<Triple>> tasks = new HashMap<>();
        int capacity = 100;

        Node D = NodeFactory.createURI("env://D");

        Map<String, Template> idToTemplate = new LinkedHashMap<>();

        idToTemplate.put("qc3", new Template(BasicPattern.wrap(Arrays.asList(
                        new Triple(D, VOID.classPartition.asNode(), Vars.k),
                        new Triple(Vars.k, VOID.classes.asNode(), Vars.c)))));

        Stopwatch rootSw = Stopwatch.createStarted();
        Flowable<Binding> root =
                QueryFlowOps.wrapClosableIteratorSupplier(() -> graph.find())
                .compose(RxUtils.counter("root", counterInterval))
//              .compose(RxUtils.queuedObserveOn(Schedulers.newThread(), capacity))

//                Flowable.fromIterable(() -> graph.find())
//                .subscribeOn(Schedulers.io())
                .map(t -> {
                    BindingMap r = new BindingHashMap();
                    r.add(Vars.s, t.getSubject());
                    r.add(Vars.p, t.getPredicate());
                    r.add(Vars.o, t.getObject());
                    return r;
                })
                .doOnComplete(() -> {
                    System.err.println("Root elapsed time: " + rootSw.elapsed(TimeUnit.MILLISECONDS) * 0.001);
                    //throw new RuntimeException("Dead");
//                    System.exit(1);
                })
//              .compose(RxUtils.queuedObserveOn(Schedulers.newThread(), capacity))
                .map(x -> {
//                    System.err.println("root: produced: " + x);
//                    System.err.println("root: produced item");
                    return x;
                })
                ;

        //root.subscribe(x -> System.out.println("Saw triple: " + x));
////        root.subscribe(x -> System.out.println("Saw triple: " + x));
//        root.toList().blockingGet();

//        if(true) {
//            Thread.sleep(3000);
//            System.out.println("Exited");
//            System.exit(0);
//        }

        // Flowable<Binding> l1 = root.share();//.subscribeOn(workerScheduler);
        Stopwatch sw = Stopwatch.createUnstarted();
        int i[] = {0};

        ConnectableFlowable<Binding> pl1 = root
                .map(x -> {
//                    System.out.println("yielding from root");
                    ++i[0];
                    if(i[0] % throughputUpdateInterval == 0) {
                        if(!sw.isRunning()) {
                            sw.start();
                            i[0] = 0;
                        } else {
                            System.err.println("Throughput of triples/second since start: " + i[0] / (sw.elapsed(TimeUnit.MILLISECONDS) * 0.001f) + " (update interval = " + throughputUpdateInterval + ")");
                        }
                    }
                    return x;
                })
//                .observeOn(workerScheduler)
//                .share()
                .compose(RxUtils.queuedObserveOn(Schedulers.newThread(), capacity))
//                .share()
                .publish()
                ;

        // publisher.subscribe(y -> System.out.println("Listener 1" + y));
        // Flowable<Binding> counter = publisher.compose(accGroupBy);

        // Turn {?s ?p ?o} into {?s a ?t}
//        ConnectableFlowable<Binding> pl11 = pl1
            Flowable<Binding> pl11 = pl1
                 .compose(RxUtils.counter("pl11", counterInterval))

//                .subscribeOn(workerScheduler)
//                .observeOn(workerScheduler)
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                .filter(QueryFlowOps
                        .createFilter(execCxt, "?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")::test)
                .compose(QueryFlowOps.transformerFromQuery(
                        "SELECT (IRI(CONCAT('x-cp://', ENCODE_FOR_URI(STR(?o)))) AS ?k) ?s (?o AS ?t) {}"))
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                .share()
                //.publish();
                ;
            // join of ?s a ?t with ?s a ?o
            tasks.computeIfAbsent("qc3", key -> pl11

//                    .doOnComplete(() -> { System.err.println("CompletedZ " + key); })
                    .compose(RxUtils.counter(key, counterInterval))
                    .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))

//                    .map(x -> { System.out.println("Working on " + key); return x; })
                    .map(x -> {
//                        Thread.sleep(1000);
//                        System.err.println(key + ": got " + x);
                        return x;
                    })
//                .compose(RxUtils.queuedObserveOn(Schedulers.newThread(), capacity))
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .doOnNext(x -> System.err.println("Join yeld: " + x))
        .concatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.s, RDF.Nodes.type, Vars.o))::apply)
        .compose(QueryFlowOps.transformerFromQuery("SELECT ?k ?t (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?t"))
        .concatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply)
                );

        RxWorkflow<Triple> result = new RxWorkflow<>(pl1, tasks);

        return result;
    }
}
