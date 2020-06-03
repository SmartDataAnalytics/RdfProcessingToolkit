package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * List the top n named graphs
 *
 * @author raven
 *
 */
@Parameters(commandDescription="Yield items (not) satisfying a given condition")
public class CmdNgsFilter {
    /**
     * sparql-pattern file
     *
     */
    @Parameter(names={"--sparql"}, description="Ask/Select/Construct query. True or non-empty result set / graph aborts the stream.")
    public String sparqlCondition;

    @Parameter(names={"-h", "--help"}, help = true)
    public boolean help = false;

    @Parameter(names={"-d", "--drop"}, description="Invert filter condition; drops matching graphs instead of keeping them")
    public boolean drop = false;

    @Parameter(names={"-o", "--out-format"})
    public String outFormat = "trig/pretty";

    @Parameter(description="Non option args")
    public List<String> nonOptionArgs = new ArrayList<>();
}
