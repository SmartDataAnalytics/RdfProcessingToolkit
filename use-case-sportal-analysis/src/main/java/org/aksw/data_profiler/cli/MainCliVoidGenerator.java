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


/**
 * A workflow for serving multiple labeled (by string) sinks at once.
 *
 * @author raven
 *
 * @param <T>
 */
class RxWorkflow<T> {
    protected ConnectableFlowable<?> rootFlowable;
    protected Map<String, Flowable<T>> tasks;

    public RxWorkflow(ConnectableFlowable<?> rootFlowable, Map<String, Flowable<T>> tasks) {
        super();
        this.rootFlowable = rootFlowable;
        this.tasks = tasks;
    }

    public ConnectableFlowable<?> getRootFlowable() {
        return rootFlowable;
    }

    public Map<String, Flowable<T>> getTasks() {
        return tasks;
    }
}


class MySinkTriplesToGraph
    extends SinkTriplesToGraph
{
    public MySinkTriplesToGraph(Graph graph) {
        super(false, graph);
    }

    public Graph getGraph() {
        return graph;
    }
}

@Command(name = "example", mixinStandardHelpOptions = true, version = "Picocli example 4.0")
public class MainCliVoidGenerator
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
            int throughputUpdateInterval = 1000;
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

        inGraph = new GraphFromSubjectCache(inGraph);

        boolean copyToMemGraph = false;

        if(copyToMemGraph) {
            Graph tmp = GraphFactory.createDefaultGraph();
            GraphUtil.addInto(tmp, inGraph);
            inGraph = tmp;
        }


        boolean testGraphFind = false;
        if(testGraphFind) {
            ExtendedIterator<Triple> it = inGraph.find();
            while(it.hasNext()) {
                Triple t = it.next();
//                System.out.println(t);
                //Thread.sleep(100);
                ExtendedIterator<Triple> join = inGraph.find(t.getSubject(), RDF.Nodes.type, Node.ANY);
                while(join.hasNext()) {
                    Triple u = join.next();
//                    System.out.println(u);
                }

            }
            it.close();

            return;
        }


        boolean debugJoin = false;
        if(debugJoin) {
            Stopwatch sw = Stopwatch.createStarted();
            Graph ffs = inGraph;
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
        Scheduler workerScheduler = Schedulers.from(executor);

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
            task
//                .observeOn(workerScheduler)
//                .observeOn(Schedulers.computation())
                .doOnNext(item -> System.err.println("Seen " + key + " on thread " + Thread.currentThread()))
                .subscribe(sink::send, err -> {}, () -> future.complete(null));
            futures.add(future);
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
         int exitCode = new CommandLine(new MainCliVoidGenerator()).execute(args);
         System.exit(exitCode);
    }


    public static RxWorkflow<Triple> generateDataProfileForVoid(
            Graph graph,
            Scheduler workerScheduler,
            boolean enableStarJoin,
            boolean enablePathJoin) throws Exception {

        Map<String, Flowable<Triple>> tasks = new HashMap<>();


        int capacity = 100;

        // FlowableTransformer<Binding, Binding> accGroupBy =
        // QueryFlowOps.transformerFromQuery(QueryFactory.create("SELECT (COUNT(*) + 1
        // AS ?c) { }"));

        // Category qf vocab
        Node distinctIRIReferenceSubjects = NodeFactory.createURI(VOID.NS + "distinctIRIReferenceSubjects");
        Node distinctIRIReferenceObjects = NodeFactory.createURI(VOID.NS + "distinctIRIReferenceObjects");
        Node distinctBlankNodeSubjects = NodeFactory.createURI(VOID.NS + "distinctBlankNodeSubjects");
        Node distinctBlankNodeObjects = NodeFactory.createURI(VOID.NS + "distinctBlankNodeObjects");
        Node distinctLiterals = NodeFactory.createURI(VOID.NS + "distinctLiterals");
        Node distinctBlankNodes = NodeFactory.createURI(VOID.NS + "distinctBlankNodes");
        Node distinctIRIReferences = NodeFactory.createURI(VOID.NS + "distinctIRIReferences");
        Node distinctRDFNodes = NodeFactory.createURI(VOID.NS + "distinctRDFNodes");
        Node subjectTypes = NodeFactory.createURI(VOID.NS + "subjectTypes");
        Node subjectClass = NodeFactory.createURI(VOID.NS + "subjectClass");
        Node objectClass = NodeFactory.createURI(VOID.NS + "objectClass");
        Node objectTypes = NodeFactory.createURI(VOID.NS + "objectTypes");
        Node distinctMembers = NodeFactory.createURI(VOID.NS + "distinctMembers");

        ExecutionContext execCxt = QueryFlowOps.createExecutionContextDefault();

        Map<String, TableN> idToTable = new LinkedHashMap<>();
//        idToTable.put("qa1", (TableN) TableFactory.create(Vars.spo));
//        idToTable.put("qa2", (TableN) TableFactory.create(Arrays.asList(Vars.c)));

//        Map<String, Graph> idToGraph = new LinkedHashMap<>();
//        List<String> graphIds = Arrays.asList("qb1", "qb2", "qb3", "qb4", "qb5");
//
//        for(String graphId : graphIds) {
//            idToGraph.put(graphId, GraphFactory.createDefaultGraph());
//        }
        Node D = NodeFactory.createURI("env://D");

        Map<String, Template> idToTemplate = new LinkedHashMap<>();
        idToTemplate.put("qbAllBut2", new Template(BasicPattern.wrap(Arrays.asList(
                        new Triple(D, VOID.triples.asNode(), Vars.x),
                        new Triple(D, VOID.distinctSubjects.asNode(), Vars.a),
                        new Triple(D, VOID.properties.asNode(), Vars.b),
                        new Triple(D, VOID.distinctObjects.asNode(), Vars.c)))));


        // Map<String, AccSinkTriples<T>> idToAcc = new LinkedHashMap<>();

//        idToAccGraph.put("qb1", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.triples.asNode(), Vars.x))))));
//
//        idToAccGraph.put("qb3", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.properties.asNode(), Vars.x))))));
//
//        idToAccGraph.put("qb4", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.distinctSubjects.asNode(), Vars.x))))));
//
//        idToAccGraph.put("qb5", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.distinctObjects.asNode(), Vars.x))))));


        idToTemplate.put("qb2", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, VOID.classes.asNode(), Vars.x)))));

//        idToAccGraph.put("qd1", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.propertyPartition.asNode(), b),
//                new Triple(b, VOID.property.asNode(), Vars.p))))));
//
//        idToAccGraph.put("qd2", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.propertyPartition.asNode(), b),
//                new Triple(b, VOID.property.asNode(), Vars.p),
//                new Triple(b, VOID.triples.asNode(), Vars.x))))));
//
        idToTemplate.put("qcAllBut35", new Template(
                        BasicPattern.wrap(Arrays.asList(
                                new Triple(D, VOID.classPartition.asNode(), Vars.k),
                                new Triple(Vars.k, VOID._class.asNode(), Vars.t),
                                new Triple(Vars.k, VOID.triples.asNode(), Vars.x),
                                new Triple(Vars.k, VOID.properties.asNode(), Vars.b),
                                new Triple(Vars.k, VOID.distinctObjects.asNode(), Vars.c)))));

        idToTemplate.put("qc3", new Template(BasicPattern.wrap(Arrays.asList(
                        new Triple(D, VOID.classPartition.asNode(), Vars.k),
                        new Triple(Vars.k, VOID.classes.asNode(), Vars.c)))));

//        new AccSinkTriples<>(sinkSupp.get(),
        idToTemplate.put("qc5", new Template(BasicPattern.wrap(Arrays.asList(
                        new Triple(D, VOID.classPartition.asNode(), Vars.k),
                        new Triple(Vars.k, VOID.distinctSubjects.asNode(), Vars.a)))));

        idToTemplate.put("qdAll", new Template(BasicPattern.wrap(Arrays.asList(
                        new Triple(D, VOID.propertyPartition.asNode(), Vars.l),
                        new Triple(Vars.l, VOID.property.asNode(), Vars.p),
                        new Triple(Vars.l, VOID.triples.asNode(), Vars.x),
                        new Triple(Vars.l, VOID.distinctSubjects.asNode(), Vars.a),
                        new Triple(Vars.l, VOID.distinctObjects.asNode(), Vars.c)))));

        idToTemplate.put("qeAll",  new Template(BasicPattern.wrap(Arrays.asList(
                        new Triple(Vars.k, VOID.propertyPartition.asNode(), Vars.l),
                        new Triple(Vars.l, VOID.property.asNode(), Vars.p),
                        new Triple(Vars.l, VOID.triples.asNode(), Vars.x),
                        new Triple(Vars.l, VOID.distinctSubjects.asNode(), Vars.a),
                        new Triple(Vars.l, VOID.distinctObjects.asNode(), Vars.c)))));

        idToTemplate.put("qf1", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctIRIReferenceSubjects, Vars.x)))));

        idToTemplate.put("qf2", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctBlankNodeSubjects, Vars.x)))));

        idToTemplate.put("qf3", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctIRIReferenceObjects, Vars.x)))));

        idToTemplate.put("qf4", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctLiterals, Vars.x)))));

        idToTemplate.put("qf5", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctBlankNodeObjects, Vars.x)))));

        idToTemplate.put("qf6", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctBlankNodes, Vars.x)))));

        idToTemplate.put("qf7", new Template(BasicPattern.wrap(Arrays.asList(
                new Triple(D, distinctIRIReferences, Vars.x)))));

        idToTemplate.put("qf8",new Template(BasicPattern.wrap(
                Arrays.asList(new Triple(D, distinctRDFNodes, Vars.x)))));

        idToTemplate.put("qf9", new Template(
                        BasicPattern.wrap(Arrays.asList(
                                new Triple(D, VOID.propertyPartition.asNode(), Vars.l),
                                new Triple(Vars.l, subjectTypes, Vars.k),
                                new Triple(Vars.k, subjectClass, Vars.t),
                                new Triple(Vars.k, distinctMembers, Vars.x)))));


        idToTemplate.put("qf10", new Template(BasicPattern.wrap(Arrays.asList(
                                new Triple(D, VOID.propertyPartition.asNode(), Vars.l),
                                new Triple(Vars.l, objectTypes, Vars.k),
                                new Triple(Vars.k, objectClass, Vars.t),
                                new Triple(Vars.k, distinctMembers, Vars.x)))));

//        idToAccGraph.put("qf9", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, distinctRDFNodes, Vars.a)
//                )))));

//        Dataset ds = DatasetFactory.create();
//        ds.asDatasetGraph().add(Quad.defaultGraphIRI, RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type);
//        ds.asDatasetGraph().add(Quad.defaultGraphIRI, RDF.Nodes.type, RDFS.Nodes.label,
//                NodeFactory.createLiteral("test"));

        // Graph graph = ds.getDefaultModel().getGraph();

        // SparqlQueryConnection conn = RDFConnectionFactory.connect(ds);

        // Query rootQuery = QueryFactory.create("SELECT * { ?s ?p ?o }");

        // Flowable<Binding> root = SparqlRx.execSelectRaw(() -> QueryExecutionFactory.create(spoQuery, DatasetGraphFactory.wrap(graph))); //rootQuery, () -> conn);

        Flowable<Binding> root =
                QueryFlowOps.wrapClosableIteratorSupplier(() -> graph.find())
//                Flowable.fromIterable(() -> graph.find())
//                .subscribeOn(Schedulers.io())
                .map(t -> {
                    BindingMap r = new BindingHashMap();
                    r.add(Vars.s, t.getSubject());
                    r.add(Vars.p, t.getPredicate());
                    r.add(Vars.o, t.getObject());
                    return r;
                })
                .map(x -> {
//                    System.err.println("root: produced: " + x);
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
        int throughputUpdateInterval = 100000;
        ConnectableFlowable<Binding> pl1 = root
                .map(x -> {
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
                .publish();

        // publisher.subscribe(y -> System.out.println("Listener 1" + y));
        // Flowable<Binding> counter = publisher.compose(accGroupBy);

        // Take one triple of spo

        if(false) {
            ConnectableFlowable<Binding> pl1one = pl1.take(1l).share().publish();
            pl1one.subscribe(idToTable.get("qa1")::addBinding);
            pl1one.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(*) AS ?c) {}"))
                    .subscribe(idToTable.get("qa2")::addBinding);
            pl1one.connect();
        }


//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(*) AS ?x) WHERE {}")).subscribe(idToAccGraph.get("qb1")::accumulate);
//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?p) AS ?x) {}")).subscribe(idToAccGraph.get("qb3")::accumulate);
//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}")).subscribe(idToAccGraph.get("qb4")::accumulate);
//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}")).subscribe(idToAccGraph.get("qb5")::accumulate);

        boolean identityMapping = false;
        if(identityMapping) {
        tasks.computeIfAbsent("test", key -> pl1
              .observeOn(workerScheduler)
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
              .flatMap(b -> QueryFlowOps.createMapperTriples(new Template(BasicPattern.wrap(Arrays.asList(
                      new Triple(Vars.s, Vars.p, Vars.o)
               )))).apply(b)));
        }
if(true) {
        tasks.computeIfAbsent("qbAllBut2", key -> pl1
//              .doOnNext(x -> System.err.println("got here: " + x))
              //.observeOn(workerScheduler)
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//        	  .doOnNext(x -> System.out.println(key + " on thread " + Thread.currentThread()))
//              .subscribeOn(workerScheduler)
              .compose(QueryFlowOps.transformerFromQuery(
              "SELECT (COUNT(?s) AS ?x) (COUNT(DISTINCT ?s) AS ?a) (COUNT(DISTINCT ?p) AS ?b) (COUNT(DISTINCT ?o) AS ?c) WHERE {}"))
              .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply))
              ;
}
        // Turn {?s ?p ?o} into {?s a ?t}
        ConnectableFlowable<Binding> pl11 = pl1
//                .subscribeOn(workerScheduler)
//                .observeOn(workerScheduler)
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                .filter(QueryFlowOps
                        .createFilter(execCxt, "?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")::test)
                .compose(QueryFlowOps.transformerFromQuery(
                        "SELECT (IRI(CONCAT('x-cp://', ENCODE_FOR_URI(STR(?o)))) AS ?k) ?s (?o AS ?t) {}"))
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                .share().publish();
if(true) {

        // pl11.subscribe(x -> System.out.println("PEEK: " + x));
        // (D classes ?x)
        tasks.computeIfAbsent("qb2", key -> pl11
//                .subscribeOn(workerScheduler)
//                .observeOn(workerScheduler)
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .doOnNext(x -> System.out.println(key + " on thread " + Thread.currentThread()))
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?t) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply))
                ;
}

if(true) {
        // single pattern for qcx: qc1, qc5 - the rest are star-joins
        // (D classPartition ?k) (?k distinctSubjects ?a)
        tasks.computeIfAbsent("qc5", key -> pl11
//                .subscribeOn(workerScheduler)
//                .observeOn(workerScheduler)
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .doOnNext(x -> System.out.println(key + " on thread " + Thread.currentThread()))
                .compose(QueryFlowOps.transformerFromQuery("SELECT ?k ?t (COUNT(DISTINCT ?s) AS ?a) {} GROUP BY ?k ?t"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));
}

    if(enableStarJoin) {
            // join of ?s a ?t with ?s a ?o
            tasks.computeIfAbsent("qc3", key -> pl11
                    .map(x -> {
//                        Thread.sleep(1000);
//                        System.err.println(key + ": got " + x);
                        return x;
                    })
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                .flatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.s, RDF.Nodes.type, Vars.o))::apply)
//                .doOnNext(x -> System.err.println("Join yeld: " + x))
                .compose(QueryFlowOps.transformerFromQuery("SELECT ?k ?t (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?t"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

    if(true) {
            // qcAllBut35
            ConnectableFlowable<Binding> pl11x = pl11
//                    .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                .flatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.s, Vars.p, Vars.o))::apply).share()
                .publish();

            tasks.computeIfAbsent("qcAllBut35", key -> pl11x
                    .compose(QueryFlowOps.transformerFromQuery(
                    "SELECT ?k ?t (COUNT(?s) AS ?x) (COUNT(DISTINCT ?p) AS ?b) (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?t"))
    //        .doOnNext(x -> System.out.println("Saw: " + x))
                    .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

            tasks.computeIfAbsent("qeAll", key -> pl11x
//                    .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                    .compose(QueryFlowOps.transformerFromQuery(
                    "SELECT ?k ?p (IRI(CONCAT(STR(?k), '-', ENCODE_FOR_URI(STR(?p)))) AS ?l) (COUNT(?s) AS ?x) (COUNT(DISTINCT ?s) AS ?a) (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?p"))
                    .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

            tasks.computeIfAbsent("qf9", key -> pl11x
//                    .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                    .compose(QueryFlowOps.transformerFromQuery(
                    "SELECT ?t (IRI(CONCAT('x-pp://', ENCODE_FOR_URI(STR(?p)))) AS ?l) (IRI(CONCAT('x-ppcp://', ENCODE_FOR_URI(STR(?p)), '-', ENCODE_FOR_URI(STR(?t)))) AS ?k) (COUNT(?s) AS ?x) {} GROUP BY ?p ?t"))
                    .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

            pl11x.connect();
    }
        }

        pl11.connect();
//        CONSTRUCT { <D> v:classes ?x } {
//        	  SELECT (COUNT(DISTINCT ?o) AS ?x) WHERE { ?s a ?o }
//        	}

        // qdx

        // qd1 subsumed by qd2
        // pl1.subscribe(idToAccGraph.get("qd1")::accumulate);

if(true) {
        tasks.computeIfAbsent("qdAll", key -> pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
//                .doOnNext(x -> System.out.println(key + " on thread " + Thread.currentThread()))
                .compose(QueryFlowOps.transformerFromQuery(
                "SELECT ?p (IRI(CONCAT('x-pp://', ENCODE_FOR_URI(STR(?p)))) AS ?l) (COUNT(?o) AS ?x) (COUNT(DISTINCT ?s) AS ?a) (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?p"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));
}

if(true) {

        tasks.computeIfAbsent("qf1", key -> pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
//                .doOnNext(x -> System.out.println(key + " on thread " + Thread.currentThread()))
                .filter(QueryFlowOps.createFilter(execCxt, "isIri(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply)
//        		.map(x -> new Triple(RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type))
            );

}

if(true) {
        tasks.computeIfAbsent("qf2", key -> pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
//                .doOnNext(x -> System.out.println(key + " on thread " + Thread.currentThread()))
                .filter(QueryFlowOps.createFilter(execCxt, "isBlank(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply)
//        		.map(x -> new Triple(RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type))
            );
}

if(true) {

        tasks.computeIfAbsent("qf3", key -> pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
                .filter(QueryFlowOps.createFilter(execCxt, "isIri(?o)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

        tasks.computeIfAbsent("qf4", key -> pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
                .filter(QueryFlowOps.createFilter(execCxt, "isLiteral(?o)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

        tasks.computeIfAbsent("qf5", key -> pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
                .filter(QueryFlowOps.createFilter(execCxt, "isBlank(?o)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));
//        if(false) {

        // All nodes flow (expressed with bindings of ?s)
        ConnectableFlowable<Binding> allNodes = pl1
                .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
//                .observeOn(workerScheduler)
                .flatMap(t -> Flowable.just(BindingFactory.binding(Vars.s, t.get(Vars.s)),
                        BindingFactory.binding(Vars.s, t.get(Vars.p)), BindingFactory.binding(Vars.s, t.get(Vars.o))))
//            .doOnNext(peek -> System.out.println("Peek: " + peek))
                .share().publish();

        tasks.computeIfAbsent("qf6", key -> allNodes.filter(QueryFlowOps.createFilter(execCxt, "isBlank(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

        tasks.computeIfAbsent("qf7", key -> allNodes.filter(QueryFlowOps.createFilter(execCxt, "isIri(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

        tasks.computeIfAbsent("qf8", key -> allNodes.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));



        allNodes.connect();

        if(enablePathJoin) {
            ConnectableFlowable<Binding> pathJoin = pl1
                    .flatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.o, RDF.Nodes.type, Vars.t))::apply).share()
                    .publish();

            tasks.computeIfAbsent("qf10", key -> pathJoin
                    .compose(RxUtils.queuedObserveOn(workerScheduler, capacity))
                    .compose(QueryFlowOps.transformerFromQuery(
                    "SELECT (IRI(CONCAT('x-pp://', ENCODE_FOR_URI(STR(?p)))) AS ?l) (IRI(CONCAT('x-ppcp://', ENCODE_FOR_URI(STR(?p)), '-', ENCODE_FOR_URI(STR(?t)))) AS ?k) (COUNT(?o) AS ?x) ?p ?t {} GROUP BY ?p ?t"))
                    .flatMap(QueryFlowOps.createMapperTriples(idToTemplate.get(key))::apply));

            pathJoin.connect();
        }
}

        // Start the whole process
        // Disposable d = pl1.connect();

//        for (Entry<String, TableN> e : idToTable.entrySet()) {
//            String key = e.getKey();
//            TableN table = e.getValue();
//
//            System.out.println(key);
//            System.out.println(ResultSetFormatter.asText(fromTable(table, execCxt)));
//        }
//
//        for (Entry<String, AccGraph> e : idToAcc.entrySet()) {
//            String key = e.getKey();
//            AccGraph accGraph = e.getValue();
//            Graph g = accGraph.getValue();
//
//            System.out.println(key);
//            Model m = ModelFactory.createModelForGraph(g);
//            RDFDataMgr.write(System.out, m, RDFFormat.TURTLE_PRETTY);
//        }

        RxWorkflow<Triple> result = new RxWorkflow<>(pl1, tasks);

        return result;
    }
}
