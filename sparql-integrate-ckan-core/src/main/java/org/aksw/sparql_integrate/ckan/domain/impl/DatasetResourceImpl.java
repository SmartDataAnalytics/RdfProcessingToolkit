package org.aksw.sparql_integrate.ckan.domain.impl;

import java.util.Set;

import org.aksw.jena_sparql_api.utils.model.SetFromResourceAndProperty;
import org.aksw.sparql_integrate.ckan.domain.api.DatasetResource;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;

public class DatasetResourceImpl
	extends CkanEntityImpl
	implements DatasetResource
{
	public DatasetResourceImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}
	
	@Override
	public Set<Resource> getAccessURLs() {
		return new SetFromResourceAndProperty<>(this, DCAT.accessURL, Resource.class);
	}
}
