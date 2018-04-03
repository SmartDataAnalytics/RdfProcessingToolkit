package org.aksw.sparql_integrate.ckan.jena.plugin;

import org.aksw.jena_sparql_api.utils.model.SimpleImplementation;
import org.aksw.sparql_integrate.ckan.domain.api.Dataset;
import org.aksw.sparql_integrate.ckan.domain.api.DatasetResource;
import org.aksw.sparql_integrate.ckan.domain.impl.DatasetImpl;
import org.aksw.sparql_integrate.ckan.domain.impl.DatasetResourceImpl;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.RDFNode;

public class JenaPluginCkan {
	public static void init() {
		init(BuiltinPersonalities.model);		
	}
	
	public static void init(Personality<RDFNode> p) {
    	p.add(Dataset.class, new SimpleImplementation(DatasetImpl::new));
    	p.add(DatasetResource.class, new SimpleImplementation(DatasetResourceImpl::new));
    }

}
