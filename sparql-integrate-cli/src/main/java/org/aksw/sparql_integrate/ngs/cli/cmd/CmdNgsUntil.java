package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.sparql_integrate.ngs.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * List the top n named graphs
 *
 * @author raven
 *
 */
@Command(name = "until", description = "Yield items up to and including the first one that satisfies the condition")
public class CmdNgsUntil implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "--sparql" }, description = "Ask/Select/Construct query. True or non-empty result set / graph aborts the stream.")
    public String sparqlCondition;

    @Option(names = { "-o", "--out-format" })
    public String outFormat = "trig/pretty";

    // @Parameter(description="Non option args")
    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.until(this);
    }
}
