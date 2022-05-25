package org.aksw.sparql_integrate.cli;

import java.util.concurrent.TimeUnit;

import org.aksw.jenax.arq.util.node.NodeEnvsubst;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.resultset.SPARQLResultVisitor;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.algebra.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

public class SparqlStmtProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SparqlStmtProcessor.class);

    protected boolean showQuery = false;
    protected boolean usedPrefixesOnly = true;
    protected boolean showAlgebra = false;
    protected boolean logTime = false;



    public boolean isLogTime() { return logTime; }

    /**
     * Convenience flag to log execution time of sparql statements
     *
     * @param logTime
     */
    public void setLogTime(boolean logTime) { this.logTime = logTime; }

    public boolean isShowQuery() { return showQuery; }
    public void setShowQuery(boolean showQuery) { this.showQuery = showQuery; }

    public boolean isUsedPrefixesOnly() { return usedPrefixesOnly; }
    public void setUsedPrefixesOnly(boolean usedPrefixesOnly) { this.usedPrefixesOnly = usedPrefixesOnly; }

    public boolean isShowAlgebra() { return showAlgebra; }
    public void setShowAlgebra(boolean showAlgebra) { this.showAlgebra = showAlgebra; }


    public void processSparqlStmt(RDFConnection conn, SparqlStmt stmt, SPARQLResultVisitor sink) {

        stmt = SparqlStmtUtils.applyNodeTransform(stmt, x -> NodeEnvsubst.subst(x, System::getenv));

        Stopwatch sw2 = Stopwatch.createStarted();

        if(usedPrefixesOnly) {
            SparqlStmtUtils.optimizePrefixes(stmt);
            /*
            if(stmt.isQuery()) {
                Query oldQuery = stmt.getAsQueryStmt().getQuery();
                Query newQuery = oldQuery.cloneQuery();
                PrefixMapping usedPrefixes = QueryUtils.usedPrefixes(oldQuery);
                newQuery.setPrefixMapping(usedPrefixes);
                stmt = new SparqlStmtQuery(newQuery);
            } else if(stmt.isUpdateRequest()) {
                // TODO Implement for update requests
                UpdateRequest oldRequest = stmt.getUpdateRequest();
                UpdateRequest newRequest = UpdateRequestUtils.clone(oldRequest);
                PrefixMapping usedPrefixes = UpdateRequestUtils.usedPrefixes(oldRequest);
                newRequest.setPrefixMapping(usedPrefixes);
                stmt = new SparqlStmtUpdate(newRequest);
            } else {
                logger.warn("Cannot optimize prefixes for unknown SPARQL statetemnt type: " + stmt);
            }
            */
        }

        if(showQuery) {
            logger.info("Processing SPARQL Statement: " + stmt);
        }

        if(showAlgebra) {
            Op op = SparqlStmtUtils.toAlgebra(stmt);
            logger.info("Algebra: " + op);
        }

        // Apply node transforms

        SparqlStmtUtils.process(conn, stmt, null, sink);
        if(logTime) {
            logger.info("SPARQL stmt execution finished after " + sw2.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");
        }

        // sink.on
    }


}