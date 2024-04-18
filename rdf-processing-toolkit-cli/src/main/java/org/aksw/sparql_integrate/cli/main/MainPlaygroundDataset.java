package org.aksw.sparql_integrate.cli.main;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.exec.QueryExecBuilderAdapter;
import org.apache.jena.sparql.util.Context;

public class MainPlaygroundDataset {
    public static void main(String[] args) {
        Dataset ds = DatasetFactory.create();
        try (RDFConnection conn = RDFConnection.connect(ds)) {
            Context cxt;
            cxt = QueryExecBuilderAdapter.adapt(conn.newQuery()).getContext();
            System.out.println(cxt);
        }
    }
}
