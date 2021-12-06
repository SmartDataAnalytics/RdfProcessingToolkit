package org.aksw.jenax.arq.datasource;

import java.io.Closeable;
import java.util.function.Function;

import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;

/**
 * An RdfDataSource wrapper for a dataset. The connection supplier is
 * a lambda in order to allow for context mutations and query transformations.
 *
 * @author raven
 *
 */
public class RdfDataSourceFromDataset
    implements RdfDataSource
{
    protected Dataset dataset;
    protected Function<? super Dataset, ? extends RDFConnection> connSupplier;
    protected Closeable closeAction;

    public RdfDataSourceFromDataset(
            Dataset dataset,
            Function<? super Dataset, ? extends RDFConnection> connSupplier,
            Closeable closeAction) {
        super();
        this.dataset = dataset;
        this.connSupplier = connSupplier;
        this.closeAction = closeAction;
    }

    public Dataset getDataset() {
        return dataset;
    }

    @Override
    public RDFConnection getConnection() {
        return connSupplier.apply(dataset);
    }

    @Override
    public void close() throws Exception {
        if (closeAction != null) {
            closeAction.close();
        }
    }

    public static RdfDataSourceFromDataset create(Dataset dataset, Function<? super Dataset, ? extends RDFConnection> connSupplier, Closeable closeAction) {
        return new RdfDataSourceFromDataset(dataset, connSupplier, closeAction);
    }

    public static RdfDataSourceFromDataset create(Dataset dataset, boolean closeDataset) {
        return new RdfDataSourceFromDataset(dataset, RDFConnection::connect, closeDataset ? dataset::close : null);
    }
}
