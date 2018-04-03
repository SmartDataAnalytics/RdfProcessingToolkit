package org.aksw.sparql_integrate.ckan.domain.api;

import org.apache.jena.rdf.model.Resource;

public interface CkanEntity
	extends Resource
{
//	String getCkanId();
//	void setCkanId(String id);
	
	// Name is a public identifier; id is a ckan internal identifier
	String getName();
	void setName(String name);

	String getTitle();
	void setTitle(String title);

	String getDescription();
	void setDescription(String description);
}
