package org.aksw.named_graph_stream.cli.cmd;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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
@Command(name = "head", description = "List or skip the first n named graphs")
public class CmdNgsHead implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "-n" }, parameterConsumer = ConsumerNumRecords.class, description = "numRecords")
    public Entry<Boolean, Long> numRecords = new SimpleEntry<>(false, 10l);

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
        return NgsCmdImpls.head(this);
    }
}
