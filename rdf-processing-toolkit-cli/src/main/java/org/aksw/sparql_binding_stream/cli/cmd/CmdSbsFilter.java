package org.aksw.sparql_binding_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.sparql_binding_stream.cli.main.SbsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "filter", description = "Filter bindings by an expression")
public class CmdSbsFilter
    implements Callable<Integer>
{
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "-e", "--expr" }, description = "expressions")
    public List<String> exprs;
    // public long numRecords = 10;

    @Option(names = { "-o", "--out-format" })
    public String outFormat = "srj";

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return SbsCmdImpls.filter(this);
    }

}
