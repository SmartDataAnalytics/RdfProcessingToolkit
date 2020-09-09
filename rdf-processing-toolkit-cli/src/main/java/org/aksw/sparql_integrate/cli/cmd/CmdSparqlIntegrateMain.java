package org.aksw.sparql_integrate.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;

import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;
import org.aksw.sparql_integrate.cli.main.SparqlIntegrateCmdImpls;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "sparql-integrate",
    versionProvider = VersionProviderRdfProcessingToolkit.class,
    description = "Run sequences of SPARQL queries and stream triples, quads and bindings")
public class CmdSparqlIntegrateMain
    implements Callable<Integer>
{
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "-v", "--version" }, versionHelp = true)
    public boolean version = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "-a", "--algebra" }, description = "Show Algebra")
    public boolean showAlgebra = false;
    // public long numRecords = 10;

    @Option(names = { "-u", "--u" }, description = "Union default graph mode")
    public boolean unionDefaultGraph = false;

    // TODO Make port configurable
    @Option(names = { "--server" }, description = "Start a SPARQL server")
    public boolean server = false;

    @Option(names = { "--port" }, description = "Server port")
    public int serverPort = 7531;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    public OutputSpec outputSpec;

    public static class OutputSpec {
        /**
         * sparql-pattern file
         *
         */
        @Option(names = { "--o" }, description = "output file") // legacy option
        public String outFile;

        @Option(names = { "--io", },  description = "input/output file")
        public String inOutFile = null;
    }


    /**
     * If not given, the output mode (quads/bindings/json) is chosen from the remaining arguments and
     * the outFormat becomes the default format of that mode
     *
     * If given, the output mode is determined by the argument
     *
     */
    @Option(names = { "-o", "--out-format", "--w" }, description = "Output format")
    public String outFormat = null;

    // Subsume jq stuff under -w jq ?

    /**
     * jq mode transforms result sets into a lossy json representation by expanding its mentioned resources up to a given depth
     * this is convenient to process in bash pipes
     *
     */
    @Option(names = { "--jq" }, parameterConsumer = ConsumeDepthValue.class, arity="0..1", fallbackValue = "3", description = "Enable jq mode")
    public Integer jqDepth = null;



    /**
     *
     *
     */
    @Option(names = { "--flat" }, description = "Suppress JSON arrays for single valued properties")
    public boolean jqFlatMode = false;

    /**
     * --jq may be followed by an integer - picocli seems to greedily parse any argument even if it is not an integer
     *
     * @author raven
     *
     */
    static class ConsumeDepthValue implements IParameterConsumer {
        @Override
        public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
            if (!args.isEmpty()) {
                String top = args.peek();
                Integer val;
                try {
                    val = Integer.parseInt(top);
                    args.pop();
                } catch(NumberFormatException e) {
                    val = 3;
                }

                argSpec.setValue(val);
            }
        }
    }



    @Parameters(arity = "0..*", description = "Arguments")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return SparqlIntegrateCmdImpls.sparqlIntegrate(this);
    }

}