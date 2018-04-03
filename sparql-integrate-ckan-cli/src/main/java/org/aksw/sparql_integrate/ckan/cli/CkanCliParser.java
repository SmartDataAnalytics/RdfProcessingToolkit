package org.aksw.sparql_integrate.ckan.cli;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSpec;

public class CkanCliParser {
//
//    private static final Logger logger = LoggerFactory.getLogger(CkanCliParser.class);
//
//
//    protected OptionParser parser = new OptionParser();
//
//    //protected Map<String, Mapper> logFmtRegistry;
//    protected Map<String, Function<InputStream, Stream<Resource>>> logFmtRegistry;
//
//    protected OptionSpec<String> inputOs;
//    protected OptionSpec<File> outputOs;
//    protected OptionSpec<String> logFormatOs;
//    protected OptionSpec<String> outFormatOs;
//    protected OptionSpec<String> rdfizerOs;
//    protected OptionSpec<String> benchmarkEndpointUrlOs;
//    protected OptionSpec<String> graphUriOs;
//    protected OptionSpec<String> datasetLabelOs;
//    protected OptionSpec<Long> headOs;
//    protected OptionSpec<Long> datasetSizeOs;
//    protected OptionSpec<Long> timeoutInMsOs;
//    protected OptionSpec<String> baseUriOs;
//    protected OptionSpec<Void> logIriAsBaseIriOs;
//    protected OptionSpec<String> queryIdPatternOs;
//    protected OptionSpec<String> datasetEndpointUriOs;
//    protected OptionSpec<String> expBaseUriOs;
//    protected OptionSpec<String> fedEndpointsOs;
//    protected OptionSpec<File> fedEndpointsFileOs;
//
//    public CkanCliParser() {
//        initOptionSpecs(parser);
//    }
//
//    public void initOptionSpecs(OptionParser parser) {
//
//        inputOs = parser
//                .acceptsAll(Arrays.asList("a", "apikey"), "")
//                .withRequiredArg()
//                //.ofType(File.class)
//                ;
//
//        outputOs = parser
//                .acceptsAll(Arrays.asList("o", "output"), "File where to store the output data.")
//                .withRequiredArg()
//                .ofType(File.class)
//                ;
//
//        logFormatOs = parser
//                .acceptsAll(Arrays.asList("m", "format"), "Format of the input data. Available options: " + logFmtRegistry.keySet())
//                .withOptionalArg()
//                .defaultsTo("combined")
//                ;
//
//        outFormatOs = parser
//                .acceptsAll(Arrays.asList("w", "outformat"), "Format for (w)riting out data. Available options: " + RDFWriterRegistry.registered())
//                .withRequiredArg()
//                .defaultsTo("Turtle/blocks")
//                ;
//
//        rdfizerOs = parser
//                .acceptsAll(Arrays.asList("r", "rdfizer"), "RDFizer selection: Any combination of the letters (e)xecution, (l)og, (q)uery and (p)rocess metadata")
//                .withOptionalArg()
//                .defaultsTo("elq")
//                ;
//
//        benchmarkEndpointUrlOs = parser
//                .acceptsAll(Arrays.asList("e", "endpoint"), "Local SPARQL service (endpoint) URL on which to execute queries")
//                .withRequiredArg()
//                .defaultsTo("http://localhost:8890/sparql")
//                ;
//
//        graphUriOs = parser
//                .acceptsAll(Arrays.asList("g", "graph"), "Local graph(s) from which to retrieve the data")
//                .availableIf(benchmarkEndpointUrlOs)
//                .withRequiredArg()
//                ;
//
//        datasetLabelOs = parser
//                .acceptsAll(Arrays.asList("l", "label"), "Label of the dataset, such as 'dbpedia' or 'lgd'. Will be used in URI generation")
//                .withRequiredArg()
//                .defaultsTo("mydata")
//                ;
//
//        headOs = parser
//                .acceptsAll(Arrays.asList("h", "head"), "Only process n entries starting from the top")
//                .withRequiredArg()
//                .ofType(Long.class)
//                ;
//
//        datasetSizeOs = parser
//                .acceptsAll(Arrays.asList("d", "dsize"), "Dataset size. Used in some computations. If not given, it will be queried (which might fail). Negative values disable dependent computations.")
//                .withRequiredArg()
//                .ofType(Long.class)
//                ;
//
//        timeoutInMsOs = parser
//                .acceptsAll(Arrays.asList("t", "timeout"), "Timeout in milliseconds")
//                .withRequiredArg()
//                .ofType(Long.class)
//                //.defaultsTo(60000l)
//                //.defaultsTo(null)
//                ;
//
//        baseUriOs = parser
//                .acceptsAll(Arrays.asList("b", "base"), "Base URI for URI generation")
//                .withRequiredArg()
//                .defaultsTo(LSQ.defaultLsqrNs)
//                ;
//
//        logIriAsBaseIriOs = parser
//                .acceptsAll(Arrays.asList("i", "logirisasbase"), "Use IRIs in RDF query logs as the base IRIs")
//                //.withOptionalArg()
//                //.ofType(Boolean.class)
//                //.defaultsTo(false)
//                ;
//
//        datasetEndpointUriOs = parser
//                .acceptsAll(Arrays.asList("p", "public"), "Public endpoint URL for record purposes - e.g. http://dbpedia.org/sparql")
//                .withRequiredArg()
//                //.defaultsTo("http://example.org/sparql")
//                //.defaultsTo(LSQ.defaultLsqrNs + "default-environment");
//                ;
//
//        expBaseUriOs = parser
//                .acceptsAll(Arrays.asList("x", "experiment"), "URI of the experiment environment")
//                .withRequiredArg()
//                //.defaultsTo(LSQ.defaultLsqrNs)
//                ;
//
//        fedEndpointsOs = parser
//                .acceptsAll(Arrays.asList("fed"), "URIs of federated endpoints")
//                .withRequiredArg();
//
//        fedEndpointsFileOs = parser
//                .acceptsAll(Arrays.asList("fedf"), "URIs of federated endpoints")
//                .withRequiredArg()
//                .ofType(File.class);
//
//        queryIdPatternOs = parser
//                .acceptsAll(Arrays.asList("q", "querypattern"), "Patter to parse out query ids; use empty string to use whole IRI")
//                .availableIf(logIriAsBaseIriOs)
//                .withOptionalArg()
//                //.withRequiredArg()
//                .defaultsTo("q-([^->]+)")
//                .ofType(String.class);
//
////        reuseLogIri = parser
////                .acceptsAll(Arrays.asList("b", "base"), "Base URI for URI generation")
////                .withRequiredArg()
////                .defaultsTo(LSQ.defaultLsqrNs)
////                ;
//
//    }
}