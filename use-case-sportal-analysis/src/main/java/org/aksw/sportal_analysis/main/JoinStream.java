package org.aksw.sportal_analysis.main;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.jena_sparql_api.mapper.AccGraph;
import org.aksw.jena_sparql_api.rx.SparqlRx;
import org.aksw.jena_sparql_api.rx.query_flow.QueryFlowOps;
import org.aksw.jena_sparql_api.utils.Vars;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.VOID;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;

public class JoinStream {

    /*
     * Manually cleaned output: (exclusions: "qf6", "qf7", "qf8" because of union
     * queries) l1 qa1.sparql [qa1.sparql, qa2.sparql, qb1.sparql, qb3.sparql,
     * qb4.sparql, qb5.sparql, qd1.sparql, qd2.sparql, qd3.sparql, qd4.sparql] # ?s
     * ?p ?o l11 qb2.sparql [qb2.sparql, qc1.sparql, qc5.sparql] # FILTER(?p =
     * rdf:type) l111 qc3.sparql [qc3.sparql] # ?s ?p ?d FILTER(?p = rdf:type) l112
     * qc6.sparql [qc2.sparql, qc4.sparql, qc6.sparql, qe1.sparql, qe2.sparql,
     * qe3.sparql, qe4.sparql, qf9.sparql] # ?s ?y ?z FILTER(?y = rdf:type) l113
     * qf10.sparql [qf10.sparql] # ?o ?y ?z FILTER(?y = rdf:type) l12 qf1.sparql
     * [qf1.sparql] # FILTER(isIri(?s)) l13 qf2.sparql [qf2.sparql] #
     * FILTER(isBlank(?s)) l14 qf3.sparql [qf3.sparql] # FILTER(isIri(?o)) l15
     * qf4.sparql [qf4.sparql] # FILTER(isLiteral(?o) l16 qf5.sparql [qf5.sparql] #
     * FILTER(isBlank(?o))
     */

    public static ResultSet fromTable(Table table, ExecutionContext execCxt) {
        ResultSet result = ResultSetFactory.create(table.iterator(execCxt), table.getVarNames());
        return result;
    }

    public static void main(String[] args) {
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
        idToTable.put("qa1", (TableN) TableFactory.create(Vars.spo));
        idToTable.put("qa2", (TableN) TableFactory.create(Arrays.asList(Vars.c)));

//        Map<String, Graph> idToGraph = new LinkedHashMap<>();
//        List<String> graphIds = Arrays.asList("qb1", "qb2", "qb3", "qb4", "qb5");
//
//        for(String graphId : graphIds) {
//            idToGraph.put(graphId, GraphFactory.createDefaultGraph());
//        }

        Map<String, AccGraph> idToAccGraph = new LinkedHashMap<>();
        Node D = NodeFactory.createURI("http://example.org/dataset");
        Node b = NodeFactory.createBlankNode();

        idToAccGraph.put("qbAllBut2",
                new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, VOID.triples.asNode(), Vars.x),
                        new Triple(D, VOID.distinctSubjects.asNode(), Vars.a),
                        new Triple(D, VOID.properties.asNode(), Vars.b),
                        new Triple(D, VOID.distinctObjects.asNode(), Vars.c))))));

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

        idToAccGraph.put("qb2", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, VOID.classes.asNode(), Vars.x))))));

//        idToAccGraph.put("qd1", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.propertyPartition.asNode(), b),
//                new Triple(b, VOID.property.asNode(), Vars.p))))));
//
//        idToAccGraph.put("qd2", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, VOID.propertyPartition.asNode(), b),
//                new Triple(b, VOID.property.asNode(), Vars.p),
//                new Triple(b, VOID.triples.asNode(), Vars.x))))));
//
        idToAccGraph.put("qcAllBut35",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(new Triple(D, VOID.classPartition.asNode(), Vars.k),
                                new Triple(Vars.k, VOID._class.asNode(), Vars.t),
                                new Triple(Vars.k, VOID.triples.asNode(), Vars.x),
                                // new Triple(Vars.k, VOID.classes.asNode(), Vars.d),
                                new Triple(Vars.k, VOID.properties.asNode(), Vars.b),
                                new Triple(Vars.k, VOID.distinctObjects.asNode(), Vars.c))))));

        idToAccGraph.put("qc3",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(new Triple(D, VOID.classPartition.asNode(), Vars.k),
                                new Triple(Vars.k, VOID.classes.asNode(), Vars.c))))));

        idToAccGraph.put("qc5",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(new Triple(D, VOID.classPartition.asNode(), Vars.k),
                                new Triple(D, VOID.distinctSubjects.asNode(), Vars.a))))));

        idToAccGraph.put("qdAll",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(
                                new Triple(D, VOID.propertyPartition.asNode(), Vars.l),
                                new Triple(Vars.l, VOID.property.asNode(), Vars.p),
                                new Triple(Vars.l, VOID.triples.asNode(), Vars.x),
                                new Triple(Vars.l, VOID.distinctSubjects.asNode(), Vars.a),
                                new Triple(Vars.l, VOID.distinctObjects.asNode(), Vars.c))))));

        idToAccGraph.put("qeAll",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(
                                new Triple(Vars.k, VOID.propertyPartition.asNode(), Vars.l),
                                new Triple(Vars.l, VOID.property.asNode(), Vars.p),
                                new Triple(Vars.l, VOID.triples.asNode(), Vars.x),
                                new Triple(Vars.l, VOID.distinctSubjects.asNode(), Vars.a),
                                new Triple(Vars.l, VOID.distinctObjects.asNode(), Vars.c))))));

        idToAccGraph.put("qf1", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctIRIReferenceSubjects, Vars.x))))));

        idToAccGraph.put("qf2", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctBlankNodeSubjects, Vars.x))))));

        idToAccGraph.put("qf3", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctIRIReferenceObjects, Vars.x))))));

        idToAccGraph.put("qf4",
                new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctLiterals, Vars.x))))));

        idToAccGraph.put("qf5", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctBlankNodeObjects, Vars.x))))));

        idToAccGraph.put("qf6", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctBlankNodes, Vars.x))))));

        idToAccGraph.put("qf7", new AccGraph(
                new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctIRIReferences, Vars.x))))));

        idToAccGraph.put("qf8",
                new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(new Triple(D, distinctRDFNodes, Vars.x))))));


        idToAccGraph.put("qf9",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(
                                new Triple(D, VOID.propertyPartition.asNode(), Vars.l),
                                new Triple(Vars.l, subjectTypes, Vars.k),
                                new Triple(Vars.k, subjectClass, Vars.t),
                                new Triple(Vars.k, distinctMembers, Vars.x))))));


        idToAccGraph.put("qf10",
                new AccGraph(new Template(
                        BasicPattern.wrap(Arrays.asList(
                                new Triple(D, VOID.propertyPartition.asNode(), Vars.l),
                                new Triple(Vars.l, objectTypes, Vars.k),
                                new Triple(Vars.k, objectClass, Vars.t),
                                new Triple(Vars.k, distinctMembers, Vars.x))))));

//        idToAccGraph.put("qf9", new AccGraph(new Template(BasicPattern.wrap(Arrays.asList(
//                new Triple(D, distinctRDFNodes, Vars.a)
//                )))));

        Dataset ds = DatasetFactory.create();
        ds.asDatasetGraph().add(Quad.defaultGraphIRI, RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type);
        ds.asDatasetGraph().add(Quad.defaultGraphIRI, RDF.Nodes.type, RDFS.Nodes.label,
                NodeFactory.createLiteral("test"));

        Graph graph = ds.getDefaultModel().getGraph();

        SparqlQueryConnection conn = RDFConnectionFactory.connect(ds);

        Query rootQuery = QueryFactory.create("SELECT * { ?s ?p ?o }");

        Flowable<Binding> root = SparqlRx.execSelectRaw(rootQuery, () -> conn);

        Flowable<Binding> l1 = root.share();
        ConnectableFlowable<Binding> pl1 = l1.publish();

        // publisher.subscribe(y -> System.out.println("Listener 1" + y));
        // Flowable<Binding> counter = publisher.compose(accGroupBy);

        // Take one triple of spo
        ConnectableFlowable<Binding> pl1one = pl1.take(1l).share().publish();

        pl1one.subscribe(idToTable.get("qa1")::addBinding);
        pl1one.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(*) AS ?c) {}"))
                .subscribe(idToTable.get("qa2")::addBinding);
        pl1one.connect();

//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(*) AS ?x) WHERE {}")).subscribe(idToAccGraph.get("qb1")::accumulate);
//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?p) AS ?x) {}")).subscribe(idToAccGraph.get("qb3")::accumulate);
//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}")).subscribe(idToAccGraph.get("qb4")::accumulate);
//        pl1.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}")).subscribe(idToAccGraph.get("qb5")::accumulate);

        pl1.compose(QueryFlowOps.transformerFromQuery(
                "SELECT (COUNT(?s) AS ?x) (COUNT(DISTINCT ?s) AS ?a) (COUNT(DISTINCT ?p) AS ?b) (COUNT(DISTINCT ?o) AS ?c) WHERE {}"))
                .subscribe(idToAccGraph.get("qbAllBut2")::accumulate);

        // Turn {?s ?p ?o} into {?s a ?t}
        ConnectableFlowable<Binding> pl11 = l1
                .filter(x -> QueryFlowOps
                        .createFilter(execCxt, "?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>").test(x))
                .compose(QueryFlowOps.transformerFromQuery(
                        "SELECT (IRI(CONCAT('cp://', ENCODE_FOR_URI(STR(?o)))) AS ?k) ?s (?o AS ?t) {}"))
                .share().publish();

        // pl11.subscribe(x -> System.out.println("PEEK: " + x));
        // (D classes ?x)
        pl11.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?t) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qb2")::accumulate);

        // single pattern for qcx: qc1, qc5 - the rest are star-joins
        // (D classPartition ?k) (?k distinctSubjects ?a)
        pl11.compose(QueryFlowOps.transformerFromQuery("SELECT ?k ?t (COUNT(DISTINCT ?s) AS ?a) {} GROUP BY ?k ?t"))
                .subscribe(idToAccGraph.get("qc5")::accumulate);

        // qcAllBut35
        ConnectableFlowable<Binding> pl11x = pl11
                .flatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.s, Vars.p, Vars.o))::apply).share()
                .publish();

        pl11x.compose(QueryFlowOps.transformerFromQuery(
                "SELECT ?k ?t (COUNT(?s) AS ?x) (COUNT(DISTINCT ?p) AS ?b) (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?t"))
                .subscribe(idToAccGraph.get("qcAllBut35")::accumulate);

        pl11x.compose(QueryFlowOps.transformerFromQuery(
                "SELECT ?k ?p (IRI(CONCAT(STR(?k), '-', ENCODE_FOR_URI(STR(?p)))) AS ?l) (COUNT(?s) AS ?x) (COUNT(DISTINCT ?s) AS ?a) (COUNT(DISTINCT ?p) AS ?b) (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?p"))
                .subscribe(idToAccGraph.get("qeAll")::accumulate);

        pl11x.flatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.s, RDF.Nodes.type, Vars.o))::apply)
                .compose(QueryFlowOps.transformerFromQuery("SELECT ?k ?t (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?k ?t"))
                .subscribe(idToAccGraph.get("qc3")::accumulate);

        pl11x.compose(QueryFlowOps.transformerFromQuery(
                "SELECT ?t (IRI(CONCAT('pp://', ENCODE_FOR_URI(STR(?p)))) AS ?l) (IRI(CONCAT('ppcp://', ENCODE_FOR_URI(STR(?p)), '-', ENCODE_FOR_URI(STR(?t)))) AS ?k) (COUNT(?s) AS ?x) {} GROUP BY ?p ?t"))
                .subscribe(idToAccGraph.get("qf9")::accumulate);

        pl11x.connect();

        pl11.connect();
//        CONSTRUCT { <D> v:classes ?x } {
//        	  SELECT (COUNT(DISTINCT ?o) AS ?x) WHERE { ?s a ?o }
//        	}

        // qdx

        // qd1 subsumed by qd2
        // pl1.subscribe(idToAccGraph.get("qd1")::accumulate);
        pl1.compose(QueryFlowOps.transformerFromQuery(
                "SELECT ?p (IRI(CONCAT('pp://', ENCODE_FOR_URI(STR(?p)))) AS ?l) (COUNT(?o) AS ?x) (COUNT(DISTINCT ?s) AS ?a) (COUNT(DISTINCT ?o) AS ?c) {} GROUP BY ?p "))
                .subscribe(idToAccGraph.get("qdAll")::accumulate);


        pl1.filter(QueryFlowOps.createFilter(execCxt, "isIri(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf1")::accumulate);

        pl1.filter(QueryFlowOps.createFilter(execCxt, "isBlank(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf2")::accumulate);

        pl1.filter(QueryFlowOps.createFilter(execCxt, "isIri(?o)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf3")::accumulate);

        pl1.filter(QueryFlowOps.createFilter(execCxt, "isLiteral(?o)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf4")::accumulate);

        pl1.filter(QueryFlowOps.createFilter(execCxt, "isBlank(?o)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?o) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf5")::accumulate);

        // All nodes flow (expressed with bindings of ?s)
        ConnectableFlowable<Binding> allNodes = pl1
                .flatMap(t -> Flowable.just(BindingFactory.binding(Vars.s, t.get(Vars.s)),
                        BindingFactory.binding(Vars.s, t.get(Vars.p)), BindingFactory.binding(Vars.s, t.get(Vars.o))))
//            .doOnNext(peek -> System.out.println("Peek: " + peek))
                .share().publish();

        allNodes.filter(QueryFlowOps.createFilter(execCxt, "isBlank(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf6")::accumulate);

        allNodes.filter(QueryFlowOps.createFilter(execCxt, "isIri(?s)")::test)
                .compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf7")::accumulate);

        allNodes.compose(QueryFlowOps.transformerFromQuery("SELECT (COUNT(DISTINCT ?s) AS ?x) {}"))
                .subscribe(idToAccGraph.get("qf8")::accumulate);



        allNodes.connect();

        ConnectableFlowable<Binding> pathJoin = pl1
                .flatMap(QueryFlowOps.createMapperForJoin(graph, new Triple(Vars.o, RDF.Nodes.type, Vars.t))::apply).share()
                .publish();

        pathJoin.compose(QueryFlowOps.transformerFromQuery(
                "SELECT (IRI(CONCAT('pp://', ENCODE_FOR_URI(STR(?p)))) AS ?l) (IRI(CONCAT('ppcp://', ENCODE_FOR_URI(STR(?p)), '-', ENCODE_FOR_URI(STR(?t)))) AS ?k) (COUNT(?o) AS ?x) ?p ?t {} GROUP BY ?p ?t"))
                .subscribe(idToAccGraph.get("qf10")::accumulate);


        pathJoin.connect();


        // Start the whole process
        Disposable d = pl1.connect();

        for (Entry<String, TableN> e : idToTable.entrySet()) {
            String key = e.getKey();
            TableN table = e.getValue();

            System.out.println(key);
            System.out.println(ResultSetFormatter.asText(fromTable(table, execCxt)));
        }

        for (Entry<String, AccGraph> e : idToAccGraph.entrySet()) {
            String key = e.getKey();
            AccGraph accGraph = e.getValue();
            Graph g = accGraph.getValue();

            System.out.println(key);
            Model m = ModelFactory.createModelForGraph(g);
            RDFDataMgr.write(System.out, m, RDFFormat.TURTLE_PRETTY);
        }

//        counter.subscribe(count -> System.out.println("Counter saw: " + count));
//
//        publisher.subscribe(y -> System.out.println("Listener 2" + y));
//
//
//
//

        // x.conn

        // root.share()

        // Flowable.
        // root.doOnNext(onNext);

    }
}
