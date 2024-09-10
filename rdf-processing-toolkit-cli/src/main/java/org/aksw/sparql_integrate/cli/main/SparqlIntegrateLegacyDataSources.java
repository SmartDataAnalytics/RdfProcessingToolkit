package org.aksw.sparql_integrate.cli.main;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map.Entry;

import org.aksw.commons.io.util.symlink.SymbolicLinkStrategies;
import org.aksw.difs.builder.DifsFactory;
import org.aksw.jenax.arq.engine.quad.RDFConnectionFactoryQuadForm;
import org.aksw.jenax.arq.service.vfs.ServiceExecutorFactoryRegistratorVfs;
import org.aksw.jenax.dataaccess.sparql.factory.dataengine.RdfDataEngineFromDataset;
import org.aksw.jenax.dataaccess.sparql.factory.dataset.connection.DatasetRDFConnectionFactory;
import org.aksw.jenax.dataaccess.sparql.factory.dataset.connection.DatasetRDFConnectionFactoryBuilder;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.tdb2.TDB2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.io.MoreFiles;

// Legacy data source code, subject to removal. The code was refactored into the various RdfDataSourceFactory(-related) classes
// and only here for cross-checking in case anything broke in the transition.
public class SparqlIntegrateLegacyDataSources {

    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateLegacyDataSources.class);

    public static Entry<Path, Closeable> resolveFsAndPath(String fsUri, String pathStr) throws IOException {
        Path dbPath = null;

        FileSystem fs;
        Closeable fsCloseActionTmp;
        if (fsUri != null && !fsUri.isBlank()) {
            fs = FileSystems.newFileSystem(URI.create(fsUri), Collections.emptyMap());
            fsCloseActionTmp = () -> fs.close();
        } else {
            fs = FileSystems.getDefault();
            fsCloseActionTmp = () -> {}; // noop
        }

        Closeable closeAction = fsCloseActionTmp;

        try {
            if (pathStr != null && !pathStr.isBlank()) {
                dbPath = fs.getPath(pathStr).toAbsolutePath();
//                for (Path root : fs.getRootDirectories()) {
//                    dbPath = root.resolve(pathStr);
//                    // Only consider the first root (if any)
//                    break;
//                }
            }
        } catch (Exception e) {
            try {
                closeAction.close();
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }

            throw new RuntimeException(e);
        }

        return Maps.immutableEntry(dbPath, closeAction);
    }

    /**
     * Set up a 'DataSource' for RDF.
     *
     * TODO The result should be closable as we may e.g. start a docker container here in the future
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    public static RdfDataEngineFromDataset configEngineOld(CmdSparqlIntegrateMain cmd) throws IOException {

        String engine = cmd.engine;

        // Resolve the arguments to a path in some file system (if applicable).
        // Also obtain a action for closing the fs (if needed)
        Entry<Path, Closeable> fsInfo = resolveFsAndPath(cmd.dbFs, cmd.dbPath);

        Path dbPath = fsInfo == null ? null : fsInfo.getKey();
        Closeable fsCloseAction = fsInfo == null ? () -> {} : fsInfo.getValue();


        RdfDataEngineFromDataset result;
        // TODO Create a registry for engines / should probably go to the conjure project
        if (engine == null || engine.equals("mem")) {

            if (dbPath != null) {
                fsCloseAction.close();
                throw new IllegalArgumentException("Memory engine does not accept a db-path. Missing engine?");
            }

            Context cxt = ARQ.getContext().copy();
            ServiceExecutorFactoryRegistratorVfs.register(cxt);

            DatasetRDFConnectionFactory connector = DatasetRDFConnectionFactoryBuilder.create()
                    .setDefaultQueryEngineFactoryProvider()
                    .setDefaultUpdateEngineFactoryProvider()
                    .setContext(cxt)
                    .build();

            result = RdfDataEngineFromDataset.create(DatasetFactory.create(), connector::connect, null);

        } else if (engine.equalsIgnoreCase("tdb2")) {

            boolean createdDbDir = false;

            if (dbPath == null) {
                Path tempDir = Paths.get(cmd.tempPath);
                dbPath = Files.createTempDirectory(tempDir, "sparql-integrate-tdb2-").toAbsolutePath();
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
                if (cmd.dbKeep) {
                    logger.info("Created new directory (will be kept after done): " + dbPath);
                    deleteAction = () -> {};
                } else {
                    logger.info("Created new directory (its content will deleted when done): " + dbPath);
                    deleteAction = () -> MoreFiles.deleteRecursively(finalDbPath);
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

                result = RdfDataEngineFromDataset.create(
                        dataset,
                        RDFConnectionFactory::connect,
                        finalDeleteAction);
            } catch (Exception e) {
                partialCloseAction.close();
                throw new RuntimeException(e);
            }

        } else if (engine.equalsIgnoreCase("difs")) {

            if (dbPath == null) {
                throw new IllegalArgumentException("Difs engine requires a db-path");
            }

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

            result = RdfDataEngineFromDataset.create(dataset,
                    ds -> RDFConnectionFactoryQuadForm.connect(ds, cxt), fsCloseAction);

        } else {
            throw new RuntimeException("Unknown engine: " + engine);
        }
        return result;
    }

}
