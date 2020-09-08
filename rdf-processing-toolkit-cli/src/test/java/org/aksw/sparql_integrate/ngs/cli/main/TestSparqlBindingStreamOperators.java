package org.aksw.sparql_integrate.ngs.cli.main;

import org.aksw.rdf_processing_toolkit.cli.main.MainCliRdfProcessingToolkit;
import org.apache.jena.sys.JenaSystem;
import org.junit.Test;

public class TestSparqlBindingStreamOperators {

    @Test
    public void test1() {
        JenaSystem.init();

        // TODO Make the output stream of the mainCore functions configurable so that we can intercept the results

        // cat js-query-3.srj | bs query 'SELECT ...'
        // bs query -o txt -q 'SELECT (SUM(?X) AS ?sum) (<foo> AS ?Y) {}' js-query-3.srj
        MainCliRdfProcessingToolkit.mainCore(new String[] {"sbs", "query", "-o", "txt", "-q", "SELECT * {}", "js-query-3.srj"});
        MainCliRdfProcessingToolkit.mainCore(new String[] {"sbs", "query", "-o", "txt", "-q", "SELECT (SUM(?X) AS ?sum) {}", "js-query-3.srj"});
        MainCliRdfProcessingToolkit.mainCore(new String[] {"sbs", "query", "-o", "txt", "-q", "SELECT (COUNT(*) AS ?count) {}", "js-query-3.srj"});
        MainCliRdfProcessingToolkit.mainCore(new String[] {"sbs", "query", "-o", "txt", "-q", "SELECT (SUM(?X) AS ?sum) (<foo> AS ?Y) {}", "js-query-3.srj"});

//        MainCliRdfProcessingToolkit.mainCore(new String[] {"sbs", "query", "-o", "txt", "-q", "SELECT ?x (SUM(?v + 1) AS ?s) {} GROUP BY ?x ORDER BY DESC(SUM(?v))", "/home/raven/Projects/EclipseOld2/jena-asf/jena-arq/testing/ARQ/Optimization/opt-top-03.srj"});
//      MainCliRdfProcessingToolkit.mainCore(new String[] {"sbs", "query", "-o", "txt", "-q", "SELECT COUNT(*) {}"});

    }

}