package org.aksw.sparql_binding_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import org.aksw.sparql_binding_stream.cli.main.SbsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "split", description = "Split/partition bindings into separate files")
public class CmdSbsSplit {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

//    @Option(names = { "-s", "--sparql" }, description = "SPARQL statement; only queries allowed")
//    public List<String> queries;
//
//    @Option(names = { "--by" }, description = "Split/Partition by the given variable name (w/o leading '?')")
//    public String varName = "srj";
//
//
//    @Option(names = { "-o", "--out-format" })
//    public String outFormat = "srj";
//
//    @Parameters(arity = "0..*", description = "Input files")
//    public List<String> nonOptionArgs = new ArrayList<>();
//
//    @Override
//    public Integer call() throws Exception {
//        return SbsCmdImpls.query(this);
//    }

}
