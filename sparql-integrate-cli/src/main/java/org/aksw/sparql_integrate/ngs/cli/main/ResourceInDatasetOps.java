package org.aksw.sparql_integrate.ngs.cli.main;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.io.json.GraphNameAndNode;
import org.aksw.jena_sparql_api.io.json.GroupedResourceInDataset;
import org.aksw.jena_sparql_api.utils.model.ResourceInDataset;
import org.aksw.jena_sparql_api.utils.model.ResourceInDatasetImpl;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;

import com.github.davidmoten.rx2.flowable.Transformers;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

public class ResourceInDatasetOps {

	public static Function<Dataset, GroupedResourceInDataset> mapToGroupedResourceInDataset(Query nodeSelector) {
	
		// TODO Ensure that the query result has two columns
		Function<? super SparqlQueryConnection, Collection<List<Node>>> mapper = ResultSetMappers.createTupleMapper(nodeSelector);
		
		return dataset -> {
			try(SparqlQueryConnection conn = RDFConnectionFactory.connect(dataset)) {
				Collection<List<Node>> tuples = mapper.apply(conn);
				
				List<GraphNameAndNode> gan = tuples.stream()
					.map(tuple -> {
						Node g = tuple.get(0);
						Node node = tuple.get(1);
						
						String graphName = g.getURI();
						
						return new GraphNameAndNode(graphName, node);
					})
					.collect(Collectors.toList());
				
				GroupedResourceInDataset r = new GroupedResourceInDataset(dataset, gan);
				return r;
			}
		};
	}

	/**
	 * Accumulate consecutive ResourceInDataset items which share the same
	 * Dataset or underlying DatasetGraph by reference equality into an
	 * Entry<Dataset, List<Node>>
	 * 
	 * @return
	 */
	public static FlowableTransformer<ResourceInDataset, GroupedResourceInDataset> groupedResourceInDataset() {
		return upstream -> upstream 	
				.compose(Transformers.<ResourceInDataset>toListWhile(
			            (list, t) -> {
			            	boolean r = list.isEmpty();
			            	if(!r) {
			            		ResourceInDataset proto = list.get(0);
			            		r = proto.getDataset() == t.getDataset() ||
			            			proto.getDataset().asDatasetGraph() == t.getDataset().asDatasetGraph();
			            	}
			            	return r;
			            }))
			    .map(list -> {
	        		ResourceInDataset proto = list.get(0);
	        		Dataset ds = proto.getDataset();
	        		List<GraphNameAndNode> nodes = list.stream()
	        				.map(r -> new GraphNameAndNode(r.getGraphName(), r.asNode()))
	        				.collect(Collectors.toList());
	        		
	        		return new GroupedResourceInDataset(ds, nodes);
			    });
	}

	public static List<ResourceInDataset> ungroupResourceInDataset(GroupedResourceInDataset grid) {
		List<ResourceInDataset> result = grid.getGraphNameAndNodes().stream()
				.map(gan -> new ResourceInDatasetImpl(grid.getDataset(), gan.getGraphName(), gan.getNode()))
				.collect(Collectors.toList());
	
		return result;
	}

	public static Flowable<ResourceInDataset> ungrouperResourceInDataset(GroupedResourceInDataset grid) {
		return Flowable.fromIterable(ungroupResourceInDataset(grid));
	}

}
