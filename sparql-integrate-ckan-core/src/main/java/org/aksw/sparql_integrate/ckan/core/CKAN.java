package org.aksw.sparql_integrate.ckan.core;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class CKAN {
	public static final String ns = "http://ckan.aksw.org/ontology/";
	
	public static Property property(String localName) {
		return ResourceFactory.createProperty(ns + localName);
	}
	
	// Link between a resource and its owning dataset
	public static final Property resource = property("resource");
}
