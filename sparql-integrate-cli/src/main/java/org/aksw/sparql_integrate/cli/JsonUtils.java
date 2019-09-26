package org.aksw.sparql_integrate.cli;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonUtils {
	public static JsonElement toJson(RDFNode rdfNode, int maxDepth) {
		JsonElement result = toJson(rdfNode, 0, maxDepth);
		return result;
	}

	public static JsonObject toJson(Resource rdfNode, int maxDepth) {
		JsonElement tmp = toJson(rdfNode, 0, maxDepth);
		JsonObject result = tmp.getAsJsonObject();
		return result;
	}
	
	public static JsonArray toJson(ResultSet rs, int maxDepth) {
		JsonArray result = new JsonArray();
		List<String> vars = rs.getResultVars();
		while(rs.hasNext()) {
			JsonObject row = new JsonObject();
			QuerySolution qs = rs.next();
			for(String var : vars) {
				RDFNode rdfNode = qs.get(var);
				JsonElement jsonElement = toJson(rdfNode, maxDepth);
				row.add(var, jsonElement);
			}
			result.add(row);
		}
		
		return result;
	}
	
	public static JsonElement toJson(RDFNode rdfNode, int depth, int maxDepth) {
		JsonElement result;

		if(depth >= maxDepth) {
			// TODO We could add properties indicating that data was cut off here
			result = new JsonObject();
		} else if(rdfNode == null) { 
			result = JsonNull.INSTANCE;
		} else if(rdfNode.isLiteral()) {
			Node node = rdfNode.asNode();
			Object obj = node.getLiteralValue();
			//boolean isNumber =//NodeMapperRdfDatatype.canMapCore(node, Number.class);
			//if(isNumber) {
			if(obj instanceof String) {
				String value = (String)obj;
				result = new JsonPrimitive(value);
			} else if(obj instanceof Number) {
				Number value = (Number)obj; //NodeMapperRdfDatatype.toJavaCore(node, Number.class);
//				Literal literal = rdfNode.asLiteral();
				result = new JsonPrimitive(value);	
			} else if(obj instanceof Boolean) {
				Boolean value = (Boolean)obj;
				result = new JsonPrimitive(value);					
			} else {
				String value = Objects.toString(obj);
				result = new JsonPrimitive(value);
//				throw new RuntimeException("Unsupported literal: " + rdfNode);
			}
		} else if(rdfNode.isResource()) {
			JsonObject tmp = new JsonObject();
			Resource r = rdfNode.asResource();
			
			if(r.isURIResource()) {
				tmp.addProperty("id", r.getURI());
				tmp.addProperty("id_type", "uri");
			} else if(r.isAnon()) {
				tmp.addProperty("id", r.getId().getLabelString());				
				tmp.addProperty("id_type", "bnode");
			}
			
			List<Statement> stmts = r.listProperties().toList();
			
			Map<Property, List<RDFNode>> pos = stmts.stream()
					.collect(Collectors.groupingBy(Statement::getPredicate,
							Collectors.mapping(Statement::getObject, Collectors.toList())));

			for(Entry<Property, List<RDFNode>> e : pos.entrySet()) {
				JsonArray arr = new JsonArray();
				Property p = e.getKey();
				String k = p.getLocalName();
				
				for(RDFNode o : e.getValue()) {
					JsonElement v = toJson(o, depth + 1, maxDepth);
					arr.add(v);
				}
				
				tmp.add(k, arr);
			}
			result = tmp;
		} else {
			throw new RuntimeException("Unknown node type: " + rdfNode);
		}
		
		return result;
	}
}
