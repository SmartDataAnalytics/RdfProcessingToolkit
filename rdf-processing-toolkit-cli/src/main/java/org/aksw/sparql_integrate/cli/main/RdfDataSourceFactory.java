package org.aksw.sparql_integrate.cli.main;

import java.util.Map;

import org.aksw.jenax.connection.datasource.RdfDataSource;

public interface RdfDataSourceFactory {
    RdfDataSource create(Map<String, Object> config) throws Exception;
}