package org.aksw.named_graph_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.named_graph_stream.cli.main.NgsCmdImpls;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "map", description = "(flat-)Map each named graph to a new set of named graphs")
public class CmdNgsMap implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;


    @ArgGroup(exclusive = true, multiplicity = "1")
    public MapSpec mapSpec;

    public static class MapSpec {
        /**
         * sparql-pattern file
         *
         */
        @Option(names = { "-s", "--sparql" }, description = "sparql file or statement(s)")
        public List<String> stmts = new ArrayList<>();

        @Option(names = { "-g", "--graph" },  description = "set the graph of triples or quads")
        public String graph = null;

        @Option(names = { "-d", "--dg", "--default-graph" },  description = "map into the default graph")
        public boolean defaultGraph = false;
    }

    @Option(names = { "-o", "--out-format" }, description = "Output format")
    public String outFormat;


    @Option(names = { "-t", "--service-timeout" }, description = "Connect and/or query timeout in ms. E.g -t 1000 or -t 1000,2000")
    public String serviceTimeout = null;

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.map(this);
    }

//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;
}
