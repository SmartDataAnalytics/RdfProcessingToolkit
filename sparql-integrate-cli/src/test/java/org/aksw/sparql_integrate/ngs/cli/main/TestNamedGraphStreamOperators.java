package org.aksw.sparql_integrate.ngs.cli.main;

import java.util.AbstractMap.SimpleEntry;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.aksw.jena_sparql_api.io.json.GroupedResourceInDataset;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSort;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Quad;
import org.junit.Assert;
import org.junit.Test;

import io.reactivex.Flowable;


public class TestNamedGraphStreamOperators {

//    @Test
//    public void testRaceCondition() {
//        Stream.generate(() -> QueryFactory.create("SELECT * { BIND(SHA256('foo') AS ?bar) }"))
//            .peek(q -> q.setResultVars()) // <-- With this line commented out, the race condition happens earlier
//             // Repeat q to increase chance to cause the race condition
//            .forEach(q -> Arrays.asList(q, q, q, q, q, q, q, q).parallelStream()
//                .forEach(query -> {
//                    Model model = ModelFactory.createDefaultModel();
//                    try(QueryExecution qe = QueryExecutionFactory.create(query, model)) {
//                        ResultSetFormatter.consume(qe.execSelect());	
//                    }
//                }));
//		
//	}
	
//	@Test
//	public void testDatasetGraphSize() {
//		Dataset ds = RDFDataMgr.loadDataset("ngs-nato-phonetic-alphabet-single-graph.nq");
//		System.out.println(ds.asDatasetGraph().size());
//		
//	}
	
//	@Test
//	public void testZipWithIndex() {
//		Flowable.fromIterable(() -> IntStream.range(0, 10).mapToObj(x -> UUID.randomUUID()).iterator())
//		.zipWith(LongStream.iterate(0, x -> x + 1)::iterator, SimpleEntry::new)
//		.forEach(System.out::println);
//			
//	}

	/**
	 * Assert that blank nodes did not get relabeled
	 */
	@Test
	public void testBlankNode() {
		Quad q = RDFDataMgrRx.createFlowableQuads("ngs-nato-phonetic-alphabet.trig", Lang.TRIG, null)
		.firstOrError()
		.blockingGet();
		
		Assert.assertEquals(NodeFactory.createBlankNode("a"), q.getSubject());
	}
	

//	@Test 
	public void testMapToGroup() {
		
		
//		Iterator<Quad> it = RDFDataMgr.createIteratorQuads(RDFDataMgr.open("ngs-nato-phonetic-alphabet.trig"), Lang.TRIG, null);
//		while(it.hasNext()) {
//			Quad q = it.next();
//			System.out.println(q.getGraph());
//		}
		
		
//		Dataset ds = RDFDataMgr.loadDataset("ngs-nato-phonetic-alphabet.trig");
//		System.out.println(ds.asDatasetGraph().size());
//		RDFDataMgr.write(System.out, ds, RDFFormat.TRIG_PRETTY);
		
		RDFDataMgrRx.createFlowableDatasets("ngs-nato-phonetic-alphabet.trig", Lang.TRIG, null)
		.map(ResourceInDatasetFlowOps
				.mapToGroupedResourceInDataset(QueryFactory.create("SELECT DISTINCT ?g ?s { GRAPH ?g { ?s ?p ?o } }"))::apply)
		.map(grid -> DatasetFlowOps.serializeForSort(DatasetFlowOps.GSON, grid.getDataset().asDatasetGraph().listGraphNodes().next(), grid))
		.map(line -> DatasetFlowOps.deserializeFromSort(DatasetFlowOps.GSON, line, GroupedResourceInDataset.class))
		.blockingForEach(x -> {
			System.out.println("Grouped " + x);
		});
	}
	
	@Test
	public void testResourceInDataset() {
		CmdNgsSort sortCmd = new CmdNgsSort();
		sortCmd.reverse = true;
		
		RDFDataMgrRx.createFlowableDatasets("ngs-nato-phonetic-alphabet.trig", Lang.TRIG, null)
		.map(ResourceInDatasetFlowOps
				.mapToGroupedResourceInDataset(QueryFactory.create("SELECT DISTINCT ?g ?s { GRAPH ?g { ?s ?p ?o } }"))::apply)
		.compose(ResourceInDatasetFlowOps.createSystemSorter(sortCmd, null))
		.flatMap(ResourceInDatasetFlowOps::ungrouperResourceInDataset)
		//.compose(DatasetStreamOps.s)
		//.compose(ResourceInDatasetFlowOps.) //FlowableOps.sysCall(SysCalls.createDefaultSortSysCall(sortCmd)))
		.compose(ResourceInDatasetFlowOps.groupedResourceInDataset())
		.flatMap(ResourceInDatasetFlowOps::ungrouperResourceInDataset)
		.blockingForEach(x -> {
			System.out.println(x);
		})
		;
		
		//.compose(MainCliNamedGraphStream.groupedResourceInDataset());
		
		
		// .compose(MainCliNamedGraphStream.createS)
		
		
		// Main
		
	}
}