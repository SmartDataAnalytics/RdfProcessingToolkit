package org.aksw.sparql_integrate.cli.main;

import java.io.Closeable;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.commons.io.util.PathUtils;
import org.aksw.commons.io.util.symlink.SymbolicLinkStrategies;
import org.aksw.difs.builder.DifsFactory;
import org.aksw.jena_sparql_api.arq.service.vfs.ServiceExecutorFactoryRegistratorVfs;
import org.aksw.jenax.arq.engine.quad.RDFConnectionFactoryQuadForm;
import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.util.Context;

public class RdfDataSourceFactoryDifs
    implements RdfDataSourceFactory
{
    @Override
    public RdfDataSource create(Map<String, Object> config) throws Exception {

        RdfDataSourceSpecBasic spec = RdfDataSourceSpecBasicFromMap.wrap(config);

        if (spec.getLocation() == null) {
            throw new IllegalArgumentException("Difs engine requires a location");
        }

        Entry<Path, Closeable> fsInfo = PathUtils.resolveFsAndPath(spec.getLocationContext(), spec.getLocation());
        Path dbPath = fsInfo.getKey();

        boolean canWrite = dbPath.getFileSystem().equals(FileSystems.getDefault());

        Context cxt = ARQ.getContext().copy();
        ServiceExecutorFactoryRegistratorVfs.register(cxt);


        Dataset dataset = DifsFactory.newInstance()
            .setUseJournal(canWrite)
            .setSymbolicLinkStrategy(SymbolicLinkStrategies.FILE)
            .setConfigFile(dbPath)
            .setCreateIfNotExists(false)
            .setMaximumNamedGraphCacheSize(10000)
            .connectAsDataset();

        RdfDataSource result = RdfDataSourceFromDataset.create(dataset,
                ds -> RDFConnectionFactoryQuadForm.connect(ds, cxt), fsInfo.getValue());

        return result;
    }
}