package org.aksw.named_graph_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.named_graph_stream.cli.main.NgsGitCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * List the top n named graphs
 *
 * @author raven
 *
 */
@Command(name = "git", description = "List all revisions of a RDF file")
public class CmdNgsGit implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    static class ConsumerNumRecords extends IParameterConsumerFlaggedLong {
        @Override
        protected String getFlag() { return "-"; };
    }

    @Option(names = { "-o", "--out-format" })
    public String outFormat = "trig/blocks";

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return NgsGitCmdImpls.git(this);
    }
}