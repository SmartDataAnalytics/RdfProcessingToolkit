package org.aksw.sparql_integrate.ckan.domain.api;

import java.util.Set;

public interface Dataset
	extends CkanEntity
{	
	Set<DatasetResource> getResources();
}
