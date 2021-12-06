package org.aksw.jenax.arq.datasource;

import java.util.Map;
import java.util.Objects;

import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.apache.jena.rdfconnection.RDFConnectionRemote;

/**
 * Use a remote sparql endpoint as a RdfDataSource
 *
 * @author raven
 *
 */
public class RdfDataSourceFactoryRemote
    implements RdfDataSourceFactory
{
    @Override
    public RdfDataSource create(Map<String, Object> config) {
        RdfDataSourceSpecBasic spec = RdfDataSourceSpecBasicFromMap.wrap(config);

        String url = Objects.requireNonNull(spec.getLocation(), "Location not set (key = " + RdfDataSourceSpecTerms.LOCATION_KEY + ")");

        RdfDataSource result = () -> RDFConnectionRemote.newBuilder()
            .destination(url)
            .build();

        return result;
    }
}