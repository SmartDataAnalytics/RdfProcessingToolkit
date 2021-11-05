package org.aksw.sparql_integrate.cli.main;

import java.io.Closeable;
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;

public class DatasetBasedSparqlEngine
    implements AutoCloseable
{
    protected Dataset dataset;
    protected Function<? super Dataset, ? extends RDFConnection> connSupplier;
    protected Closeable closeAction;

    public DatasetBasedSparqlEngine(Dataset dataset, Function<? super Dataset, ? extends RDFConnection> connSupplier, Closeable closeAction) {
        super();
        this.dataset = dataset;
        this.connSupplier = connSupplier;
        this.closeAction = closeAction;
    }

    public RDFConnection newConnection() {
        return connSupplier.apply(dataset);
    }

    @Override
    public void close() throws Exception {
        if (closeAction != null) {
            closeAction.close();
        }
    }

    public static DatasetBasedSparqlEngine create(Dataset dataset, Function<? super Dataset, ? extends RDFConnection> connSupplier, Closeable closeAction) {
        return new DatasetBasedSparqlEngine(dataset, connSupplier, closeAction);
    }

}
