package org.aksw.named_graph_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.named_graph_stream.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "cat", description = "Output and optionally convert graph input")
public class CmdNgsCat implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "-o", "--out-format" })
    public String outFormat = "trig/blocks";

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.cat(this);
    }
}
