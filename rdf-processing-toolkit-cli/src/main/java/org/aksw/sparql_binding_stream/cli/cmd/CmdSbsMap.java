package org.aksw.sparql_binding_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.sparql_binding_stream.cli.main.SbsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "map", description = "Map bindings via queries")
public class CmdSbsMap
    implements Callable<Integer>
{

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "-s", "--sparql" }, description = "SPARQL statement; only queries allowed")
    public List<String> queries;
    // public long numRecords = 10;

    @Option(names = { "-o", "--out-format" })
    public String outFormat = "srj";

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return SbsCmdImpls.query(this);
    }

}
