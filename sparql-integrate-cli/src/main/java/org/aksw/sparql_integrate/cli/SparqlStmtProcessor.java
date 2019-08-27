package org.aksw.sparql_integrate.cli;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.aksw.jena_sparql_api.stmt.SPARQLResultVisitor;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtQuery;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.utils.QueryUtils;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

public class SparqlStmtProcessor {

	private static final Logger logger = LoggerFactory.getLogger(SparqlStmtProcessor.class);

	protected boolean showQuery = false;
	protected boolean usedPrefixesOnly = true;
	protected boolean showAlgebra = false;

	public boolean isShowQuery() { return showQuery; }
	public void setShowQuery(boolean showQuery) { this.showQuery = showQuery; }

	public boolean isUsedPrefixesOnly() { return usedPrefixesOnly; }
	public void setUsedPrefixesOnly(boolean usedPrefixesOnly) { this.usedPrefixesOnly = usedPrefixesOnly; }

	public boolean isShowAlgebra() { return showAlgebra; }
	public void setShowAlgebra(boolean showAlgebra) { this.showAlgebra = showAlgebra; }
	
	public static Node substWithLookup(Node node, Function<String, String> lookup) {
		String ENV = "env:";
		
		Node result = node;
		if(node.isURI()) {
			String str = node.getURI();
			if(str.startsWith(ENV)) {
				String key = str.substring(ENV.length());

				boolean isUri = false;
				if(key.startsWith("//")) {
					key = key.substring(2);
					isUri = true;
				}

				
				String value = lookup.apply(key);
				if(!Strings.isNullOrEmpty(value)) {
					result = isUri
						? NodeFactory.createURI(value)
						: NodeFactory.createLiteral(value);
				}
			}
		}
		
		return result;
	}
	
	public void processSparqlStmt(RDFConnection conn, SparqlStmt stmt, SPARQLResultVisitor sink) {
		
		stmt = SparqlStmtUtils.applyNodeTransform(stmt, x -> SparqlStmtProcessor.substWithLookup(x, System::getenv));
		
		Stopwatch sw2 = Stopwatch.createStarted();

		if(usedPrefixesOnly) {
			if(stmt.isQuery()) {
				Query oldQuery = stmt.getAsQueryStmt().getQuery();
	        	Query newQuery = oldQuery.cloneQuery();
	        	PrefixMapping usedPrefixes = QueryUtils.usedPrefixes(oldQuery);
	        	newQuery.setPrefixMapping(usedPrefixes);
	        	stmt = new SparqlStmtQuery(newQuery);
			}
			
			// TODO Implement for update requests
		}

		if(showQuery) {
			logger.info("Processing SPARQL Statement: " + stmt);
		}
		
		if(showAlgebra) {
			Op op = toAlgebra(stmt);
			logger.info("Algebra: " + op);
		}

		// Apply node transforms
		
		SparqlStmtUtils.process(conn, stmt, sink);
		logger.info("SPARQL stmt execution finished after " + sw2.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");

	}
	
	public static Op toAlgebra(SparqlStmt stmt) {
		Op result = null;

		if(stmt.isQuery()) {
			Query q = stmt.getAsQueryStmt().getQuery();
			result = Algebra.compile(q);
		} else if(stmt.isUpdateRequest()) {
			UpdateRequest ur = stmt.getAsUpdateStmt().getUpdateRequest();
			for(Update u : ur) {
				if(u instanceof UpdateModify) {
					Element e = ((UpdateModify)u).getWherePattern();
					result = Algebra.compile(e);
				}
			}
		}

		return result;
	}
	

}