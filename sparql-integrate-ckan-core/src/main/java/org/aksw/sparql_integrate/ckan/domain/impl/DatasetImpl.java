package org.aksw.sparql_integrate.ckan.domain.impl;

import java.util.Set;

import org.aksw.jena_sparql_api.utils.model.SetFromResourceAndProperty;
import org.aksw.sparql_integrate.ckan.domain.api.Dataset;
import org.aksw.sparql_integrate.ckan.domain.api.DatasetResource;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.DCAT;

public class DatasetImpl
	extends CkanEntityImpl
	implements Dataset
{
	public DatasetImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}

	@Override
	public Set<DatasetResource> getResources() {
		return new SetFromResourceAndProperty<>(this, DCAT.distribution, DatasetResource.class);
	}

}
