package org.aksw.sparql_integrate.ckan.domain.api;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;

public interface DatasetResource
	extends CkanEntity
{
	// TODO Move to a subgrass of datasetResource
	Set<Resource> getAccessURLs();
}
