package org.aksw.named_graph_stream.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.named_graph_stream.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "subjects", description = "Group triples with consecutive subjects into named graphs")
public class CmdNgsSubjects implements Callable<Integer> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Option(names={"-o", "--out-format"})
    public String outFormat = "trig/pretty";

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.subjects(this);
    }

//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;
}
