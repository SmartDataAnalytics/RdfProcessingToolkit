package org.aksw.sparql_integrate.ckan.domain.impl;

import org.aksw.sparql_integrate.ckan.domain.api.DatasetResource;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;

public class DatasetResourceImpl
	extends CkanEntityImpl
	implements DatasetResource
{
	public DatasetResourceImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}
}
