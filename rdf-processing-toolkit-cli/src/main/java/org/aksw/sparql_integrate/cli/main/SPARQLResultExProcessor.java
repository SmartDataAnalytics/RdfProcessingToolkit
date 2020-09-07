package org.aksw.sparql_integrate.cli.main;

import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;

public interface SPARQLResultExProcessor
    extends SinkStreaming<SPARQLResultEx>, SPARQLResultExVisitor<Void>
{

}
