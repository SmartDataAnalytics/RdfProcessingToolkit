package org.aksw.sparql_integrate.cli.main;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.commons.io.util.PathUtils;
import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.tdb2.TDB2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.StandardSystemProperty;
import com.google.common.io.MoreFiles;

public class RdfDataSourceFactoryTdb2
    implements RdfDataSourceFactory
{
    private static final Logger logger = LoggerFactory.getLogger(RdfDataSourceFactoryTdb2.class);


    @Override
    public RdfDataSource create(Map<String, Object> config) throws Exception {
        RdfDataSource result;

        RdfDataSourceSpecBasic spec = RdfDataSourceSpecBasicFromMap.wrap(config);
        Entry<Path, Closeable> fsInfo = PathUtils.resolveFsAndPath(spec.getLocationContext(), spec.getLocation());

        Path dbPath = fsInfo == null ? null : fsInfo.getKey();
        Closeable fsCloseAction = fsInfo == null ? () -> {} : fsInfo.getValue();


        boolean createdDbDir = false;

        if (dbPath == null) {
            String tmpDirStr = spec.getTempDir();
            if (tmpDirStr == null) {
                tmpDirStr = StandardSystemProperty.JAVA_IO_TMPDIR.value();
            }

            if (tmpDirStr == null) {
                throw new IllegalStateException("Temp dir neither specified nor obtainable from java.io.tmpdir");
            }

            Path tmpDir = Paths.get(tmpDirStr);
            dbPath = Files.createTempDirectory(tmpDir, "sparql-integrate-tdb2-").toAbsolutePath();
            createdDbDir = true;
        } else {
            dbPath = dbPath.toAbsolutePath();
            if (!Files.exists(dbPath)) {
                Files.createDirectories(dbPath);
                createdDbDir = true;
            }
        }

        Path finalDbPath = dbPath;
        Closeable deleteAction;
        if (createdDbDir) {
            if (Boolean.TRUE.equals(spec.isAutoDeleteIfCreated())) {
                logger.info("Created new directory (its content will deleted when done): " + dbPath);
                deleteAction = () -> MoreFiles.deleteRecursively(finalDbPath);
            } else {
                logger.info("Created new directory (will be kept after done): " + dbPath);
                deleteAction = () -> {};
            }
        } else {
            logger.warn("Folder already existed - delete action disabled: " + dbPath);
            deleteAction = () -> {};
        }

        // Set up a partial close action because connecting to the db may yet fail
        Closeable partialCloseAction = () -> {
            try {
                deleteAction.close();
            } finally {
                fsCloseAction.close();
            }
        };

        Location location = Location.create(finalDbPath);
        try {
            Dataset dataset = TDB2Factory.connectDataset(location);

            logger.info("Connecting to TDB2 database in folder " + dbPath);
            Closeable finalDeleteAction = () -> {
                try {
                    dataset.close();
                } finally {
                    partialCloseAction.close();
                }
            };

            result = RdfDataSourceFromDataset.create(
                    dataset,
                    RDFConnection::connect,
                    finalDeleteAction);
        } catch (Exception e) {
            partialCloseAction.close();
            throw new RuntimeException(e);
        }

        return result;
    }
}