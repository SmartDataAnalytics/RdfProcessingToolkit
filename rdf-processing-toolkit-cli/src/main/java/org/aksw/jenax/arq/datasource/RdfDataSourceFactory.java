package org.aksw.jenax.arq.datasource;

import java.util.Map;

import org.aksw.jenax.connection.datasource.RdfDataSource;

public interface RdfDataSourceFactory {
    RdfDataSource create(Map<String, Object> config) throws Exception;
}