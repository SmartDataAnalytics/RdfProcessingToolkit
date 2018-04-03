package org.aksw.sparql_integrate.ckan.domain.impl;

import org.aksw.jena_sparql_api.utils.model.ResourceUtils;
import org.aksw.sparql_integrate.ckan.domain.api.CkanEntity;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.DCTerms;

public class CkanEntityImpl
	extends ResourceImpl
	implements CkanEntity
{
	public CkanEntityImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}
	
//	@Override
//	public String getCkanId() {
//		String result = ResourceUtils.getLiteralValue(this, DCTerms.identifier, Literal::getString).orElse(null);
//		return result;
//	}
//	
//	@Override
//	public void setCkanId(String identifier) {
//		ResourceUtils.setLiteralValue(this, DCTerms.identifier, String.class, identifier);
//	}
	
	@Override
	public String getName() {
		String result = ResourceUtils.getLiteral(this, DCTerms.identifier, Literal::getString).orElse(null);
		return result;
	}
	
	@Override
	public void setName(String identifier) {
		ResourceUtils.setLiteral(this, DCTerms.identifier, String.class, identifier);
	}

	@Override
	public String getTitle() {
		String result = ResourceUtils.getLiteral(this, DCTerms.title, Literal::getString).orElse(null);
		return result;
	}

	@Override
	public String getDescription() {
		String result = ResourceUtils.getLiteral(this, DCTerms.description, Literal::getString).orElse(null);
		return result;
	}


	@Override
	public void setTitle(String title) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub
		
	}
	
	
}
