package org.aksw.named_graph_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.named_graph_stream.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * List the top n named graphs
 *
 * @author raven
 *
 */
@Command(name = "filter", description = "Yield items (not) satisfying a given condition")
public class CmdNgsFilter implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "--sparql" }, description = "Ask/Select/Construct query. True or non-empty result set / graph aborts the stream.")
    public String sparqlCondition;


    @Option(names = { "-d", "--drop" }, description = "Invert filter condition; drops matching graphs instead of keeping them")
    public boolean drop = false;

    @Option(names = { "-o", "--out-format" })
    public String outFormat = "trig/blocks";

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.filter(this);
    }
}
