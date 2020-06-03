package org.aksw.data_profiler.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.ext.virtuoso.VirtuosoBulkLoad;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.WebContent;
import org.hobbit.core.service.docker.api.DockerService;
import org.hobbit.core.service.docker.impl.docker_client.DockerServiceDockerClient;
import org.hobbit.core.service.docker.impl.docker_client.DockerServiceSystemDockerClient;
import org.hobbit.core.service.docker.util.ComponentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.shaded.com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.spotify.docker.client.DockerClient;

import virtuoso.jdbc4.VirtuosoDataSource;

interface RunnableEx {
    void run() throws Exception;
}

public class MainCliVoidGenVirtuoso {
    private static final Logger logger = LoggerFactory.getLogger(MainCliVoidGenVirtuoso.class);


    public static Multimap<Path, Query> listQueries(Path startPath, String pattern) throws IOException {
        Predicate<Path> exclusions = x -> false;

        PathMatcher pathMatcher = startPath.getFileSystem().getPathMatcher(pattern);

        Multimap<Path, Query> result = ArrayListMultimap.create();
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(file)) {
                    boolean isExcluded = exclusions.test(file);

                    if (!isExcluded) {
                        String queryStr = Files.lines(file).collect(Collectors.joining("\n"));
                        try {
                            Query query = QueryFactory.create(queryStr);

                            result.put(file, query);
                        } catch (Exception e) {
                            // Silently ignore queries that fail to parse
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    public static void benchmark(String name, RunnableEx x) throws Exception {
        logger.info("Starting task [" + name + "]");
        Stopwatch sw = Stopwatch.createStarted();
        x.run();
        logger.info("Task [" + name + "] finished in " + sw.elapsed(TimeUnit.MILLISECONDS) * 0.001 + " seconds");
    }

    public static void main(String[] args) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        work(args);
        logger.info("Task completed  in " + sw.elapsed(TimeUnit.MILLISECONDS) * 0.001 + " seconds");
    }
        public static void work(String[] args) throws Exception {

            Multimap<Path, Query> queries = listQueries(Paths.get("src/main/resources/compact"), "glob:**/*.sparql");

            logger.info("Loaded " + queries.size() + " queries");

            try(DockerServiceSystemDockerClient dss =
                    DockerServiceSystemDockerClient
                        .create(true, Collections.emptyMap(), Collections.emptySet())) {

            DockerClient dockerClient = dss.getDockerClient();


            List<Path> paths = Arrays.asList(Paths.get("/home/raven/tmp/sorttest/yago-wd-facts-types_sorted-2m.nt"));
            //conn = DatasetFactory.wrap(model);

            logger.info("Attempting to starting a virtuoso from docker");
            DockerServiceDockerClient dsCore = dss.create("tenforce/virtuoso", ImmutableMap.<String, String>builder()
                    .put("SPARQL_UPDATE", "true")
                    .put("DEFAULT_GRAPH", "http://www.example.org/")
//                    .put("VIRT_Parameters_NumberOfBuffers", "170000")
//                    .put("VIRT_Parameters_MaxDirtyBuffers", "130000")
                    .put("VIRT_Parameters_NumberOfBuffers", "680000")
                    .put("VIRT_Parameters_MaxDirtyBuffers", "500000")
                    .put("VIRT_Parameters_MaxVectorSize", "1000000000")
                    .put("VIRT_SPARQL_ResultSetMaxRows", "1000000000")
                    .put("VIRT_SPARQL_MaxQueryCostEstimationTime", "0")
                    .put("VIRT_SPARQL_MaxQueryExecutionTime", "600")
                    .build());

            DockerService ds = null;
            try {
//                ds = dsCore;
                ds = ComponentUtils.wrapSparqlServiceWithHealthCheck(dsCore, 8890);

                ds.startAsync().awaitRunning();


                String dockerContainerId = dsCore.getCreationId();
                String dockerContainerIp = ds.getContainerId();

                //logger.info("Copying data into container");
                Path unzipFolder = Paths.get("/tmp").resolve(dockerContainerId);
                Files.createDirectories(unzipFolder);

                benchmark("DataLoading", () -> {
                    for(Path path : paths) {
                        Path filename = path.getFileName();
                        Path target = unzipFolder.resolve(filename);
                        Files.copy(path, target);
                    }
                    String allowedDir = "/usr/local/virtuoso-opensource/var/lib/virtuoso/db/";
                    dockerClient.copyToContainer(unzipFolder, dockerContainerId, allowedDir);

                    logger.info("Connecting to virtuoso");
                    VirtuosoDataSource dataSource = new VirtuosoDataSource();
                    dataSource.setUser("dba");
                    dataSource.setPassword("dba");
                    dataSource.setPortNumber(1111);
                    dataSource.setServerName(dockerContainerIp);
                    try(java.sql.Connection c = dataSource.getConnection()) {
                        logger.info("Preparing bulk loader");
                        VirtuosoBulkLoad.logEnable(c, 2, 0);

                        for(Path path : paths) {
                            String actualFilename = path.getFileName().toString();
                            logger.info("Registered file for bulkload: " + actualFilename);
                            VirtuosoBulkLoad.ldDir(c, allowedDir, actualFilename, "http://www.example.org/");
                        }

                        logger.info("Running bulk loader");
                        VirtuosoBulkLoad.rdfLoaderRun(c);

                        logger.info("Creating checkpoint");
                        VirtuosoBulkLoad.checkpoint(c);
                    }
                });



                Files.walk(unzipFolder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    //.peek(System.out::println)
                    .forEach(File::delete);
                    //.count();

    /// end of DOCKER STUFF

            String sparqlApiBase = "http://" + ds.getContainerId() + ":8890/";
            String sparqlEndpoint = sparqlApiBase + "sparql";


            benchmark("Querying", () -> {
                try(RDFConnection conn= RDFConnectionRemote.create()
                    .destination(sparqlEndpoint)
                    .acceptHeaderSelectQuery(WebContent.contentTypeResultsXML)
                    .build()) {

                    List<Entry<Path, Query>> qs = queries.entries().stream().collect(Collectors.toList());

                    List<Model> models = qs.parallelStream()
                        .map(e -> {
                            Model m;
                            String name = e.getKey().getFileName().toString();
                            Query q = e.getValue();
                            logger.info("Execution starting: " + name);
                            try(QueryExecution qe = conn.query(q)) {
                                m = qe.execConstruct();
                            }
                            logger.info("Execution finished: " + name + " with " + m.size() + " triples");
                            return m;
                        })
                        .collect(Collectors.toList());
                }
            });

//                try(QueryExecution qe = conn.query("SELECT (COUNT(*) AS ?c) { ?s ?p ?o }")) {
//                    System.out.println(ResultSetFormatter.asText(qe.execSelect()));
//                }

            } finally {
                if(ds != null) {
                    ds.stopAsync().awaitTerminated(30, TimeUnit.SECONDS);
                }
            }
    }

    }
}
