package org.aksw.jenax.arq.datasource;

import java.io.Closeable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.commons.io.util.FileUtils;
import org.aksw.commons.io.util.PathUtils;
import org.aksw.commons.io.util.symlink.SymbolicLinkStrategies;
import org.aksw.commons.util.exception.FinallyAll;
import org.aksw.difs.builder.DifsFactory;
import org.aksw.difs.system.domain.StoreDefinition;
import org.aksw.jena_sparql_api.arq.service.vfs.ServiceExecutorFactoryRegistratorVfs;
import org.aksw.jenax.arq.engine.quad.RDFConnectionFactoryQuadForm;
import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfDataSourceFactoryDifs
    implements RdfDataSourceFactory
{
    private static final Logger logger = LoggerFactory.getLogger(RdfDataSourceFactoryDifs.class);

    @Override
    public RdfDataSource create(Map<String, Object> config) throws Exception {

        RdfDataSourceSpecBasic spec = RdfDataSourceSpecBasicFromMap.wrap(config);

        if (spec.getLocation() == null) {
            throw new IllegalArgumentException("Difs engine requires the location of a store config file");
        }

        Entry<Path, Closeable> fsInfo = PathUtils.resolveFsAndPath(spec.getLocationContext(), spec.getLocation());
        Path confFile = fsInfo.getKey();

        boolean canWrite = confFile.getFileSystem().equals(FileSystems.getDefault());

        Context cxt = ARQ.getContext().copy();
        ServiceExecutorFactoryRegistratorVfs.register(cxt);


        // If the config does not exist then delete it upon completion

        Path basePath = confFile.getParent();
        if (basePath == null) {
            throw new IllegalArgumentException("Location must be a file");
        }

        // TODO Get default paths from difs factory to ensure consistency
        Path dftStorePath = basePath.resolve("store");
        Path dftIndexPath = basePath.resolve("index");

        boolean deleteWhenDone =
                Boolean.TRUE.equals(spec.isAutoDeleteIfCreated())
                && canWrite
                && !Files.exists(confFile) && !Files.exists(dftStorePath) && !Files.exists(dftIndexPath);

        if (deleteWhenDone) {
            logger.info(String.format("Creating temporary difs store with config file %s (files will be deleted when done)", confFile));
        }
//        else {
//            logger.info(String.format("Connecting to existing difs store using config %s", confFile));
//        }

        Path dftLocksPath = basePath.resolve("locks");
        Path dftTxnsPath = basePath.resolve("txns");

        // Make sure we don't accidently delete possibly unrelated locks / txns folders (better safe than sorry)
        for (Path validation : Arrays.asList(dftLocksPath, dftTxnsPath) ) {
            if (Files.exists(validation)) {
                throw new IllegalStateException("Neither store/index folders nor config file found but either orphaned or unrelated file " + validation + " existed.");
            }
        }

        Path ancestorPath = FileUtils.getFirstExistingAncestor(confFile);
        Files.createDirectories(basePath);

        // Path createDirs = ancestorPath.relativize(basePath);



        StoreDefinition defaultDefinition = ModelFactory.createDefaultModel().createResource().as(StoreDefinition.class)
                .setStorePath("store")
                .setIndexPath("index")
                .setAllowEmptyGraphs(true);

        Dataset dataset = DifsFactory.newInstance()
            .setStoreDefinition(defaultDefinition)
            .setUseJournal(canWrite)
            .setSymbolicLinkStrategy(SymbolicLinkStrategies.FILE)
            .setConfigFile(confFile)
            .setCreateIfNotExists(true)
            .setMaximumNamedGraphCacheSize(10000)
            .connectAsDataset();

        RdfDataSource result = RdfDataSourceFromDataset.create(dataset,
                ds -> RDFConnectionFactoryQuadForm.connect(ds, cxt), () -> {
                    if (deleteWhenDone) {
                        logger.info(String.format("Deleting difs files based at %s", basePath));
                        FinallyAll.run(
                            () -> FileUtils.deleteRecursivelyIfExists(dftIndexPath),
                            () -> FileUtils.deleteRecursivelyIfExists(dftStorePath),
                            () -> FileUtils.deleteRecursivelyIfExists(dftTxnsPath),
                            () -> FileUtils.deleteRecursivelyIfExists(dftLocksPath),
                            () -> Files.deleteIfExists(confFile),
                            () -> FileUtils.deleteEmptyFolders(basePath, ancestorPath, false),
                            () -> fsInfo.getValue().close()
                        );
                    }
                });

        return result;
    }
}