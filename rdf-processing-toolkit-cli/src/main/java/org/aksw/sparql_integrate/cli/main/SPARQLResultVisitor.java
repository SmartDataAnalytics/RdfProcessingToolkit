package org.aksw.sparql_integrate.cli.main;

import java.util.Iterator;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.resultset.SPARQLResult;

public interface SPARQLResultVisitor<T> {
    T onBooleanResult(Boolean value);
    T onResultSet(ResultSet it);
    T onJson(Iterator<JsonObject> it);

    default T forward(SPARQLResult sr) {
        T result;
        if (sr.isResultSet()) {
            result = onResultSet(sr.getResultSet());
        } else if (sr.isBoolean()) {
            result = onBooleanResult(sr.getBooleanResult());
        } else {
            throw new IllegalArgumentException("Unknow case " + sr);
        }

        return result;
    }
}