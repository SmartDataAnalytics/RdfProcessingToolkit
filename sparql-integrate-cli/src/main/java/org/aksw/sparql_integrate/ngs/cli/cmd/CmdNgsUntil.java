package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * List the top n named graphs
 *
 * @author raven
 *
 */
public class CmdNgsUntil {
    /**
     * sparql-pattern file
     *
     */
    @Parameter(names={"--sparql"}, description="Ask/Select/Construct query. True or non-empty result set / graph aborts the stream.")
    public String sparqlCondition;

    @Parameter(names={"-h", "--help"}, help = true)
    public boolean help = false;

    @Parameter(names={"-o", "--out-format"})
    public String outFormat = "trig/pretty";

    @Parameter(description="Non option args")
    public List<String> nonOptionArgs = new ArrayList<>();
}
