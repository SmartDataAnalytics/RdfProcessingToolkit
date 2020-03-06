package org.aksw.sparql_integrate.ngs.cli.main;

import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.Lang;
import org.junit.Test;

public class TestNamedGraphStreamOperators {

	@Test
	public void testResourceInDataset() {
		RDFDataMgrRx.createFlowableDatasets("ngs-nato-phonetic-alphabet.trig", Lang.TRIG, null)
		.map(ResourceInDatasetOps
				.mapToGroupedResourceInDataset(QueryFactory.create("SELECT DISTINCT ?g ?s { GRAPH ?g { ?s ?p ?o } }"))::apply)
		.flatMap(ResourceInDatasetOps::ungrouperResourceInDataset)
		.compose(ResourceInDatasetOps.groupedResourceInDataset())
		.blockingForEach(x -> {
			System.out.println(x);
		})
		;
		
		//.compose(MainCliNamedGraphStream.groupedResourceInDataset());
		
		
		// .compose(MainCliNamedGraphStream.createS)
		
		
		// Main
		
	}
}
