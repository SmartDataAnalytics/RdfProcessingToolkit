package org.aksw.sparql_integrate.cli;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aksw.jenax.arq.util.exception.HttpExceptionUtils;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load data via jena's RDFConnection interface
 *
 * @author raven
 *
 */
public class MainCliSparqlLoad {
    private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlLoad.class);

    public static void main(String[] args) {
        try {
            mainCore(args);
        } catch(Exception e) {
            throw HttpExceptionUtils.makeHumanFriendly(e);
        }
    }

    public static void mainCore(String[] args) {

//		Long batchSize = null;
//		String GRAPH_SET = "graph=";
//		String GRAPH_RESET = "graph";

        // Known options and their defaults
        Map<String, String> knownOptions = new HashMap<>();
        knownOptions.put("--e", null); // endpoint url
        knownOptions.put("--g", null); // graph

        Map<String, String> options = new LinkedHashMap<>();

        for(String arg : args) {
            String key = null;
            String val = null;

            int i = arg.indexOf("=");
            if(i >= 0) {
                key = arg.substring(0, i).trim();
                val = arg.substring(i + 1).trim();
            } else {
                if(knownOptions.containsKey(arg)) {
                    key = arg;
                    val = knownOptions.get(arg);
                }
            }

            if(key != null) {
                logger.info("Setting property '" + key + "' to '" + val + "'");

                if(!knownOptions.containsKey(key)) {
                    logger.error("'" + key + "' is not a known property");
                }

                options.put(key, val);
            } else {

                String serviceUrl = options.get("--e");

                if(serviceUrl == null) {
                    throw new RuntimeException("No service set. Specify one using for example: --e=http://localhost/sparql");
                }
                String graph = options.get("--g");

                try(RDFConnection conn = RDFConnectionFactory.connect(serviceUrl)) {
                    logger.info("Invoking load of file '" + arg + "' into graph '" + graph + "'");
                    if(graph == null) {
                        conn.load(arg);
                    } else {
                        logger.info("Invoking load of file '" + arg + "'");
                        conn.load(graph, arg);
                    }

                    logger.info("Loading succeeded");
                }
            }
        }

        logger.info("All successfully done.");
    }
}
