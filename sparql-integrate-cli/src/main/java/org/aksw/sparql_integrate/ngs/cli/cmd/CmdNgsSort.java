package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.sparql_integrate.ngs.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "sort", description = "Sort named graphs by key")
public class CmdNgsSort implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Option(names = { "-k", "--key" })
    public String key = null;

    @Option(names = { "-R", "--random-sort" })
    public boolean randomSort = false;

    @Option(names = { "-r", "--reverse" })
    public boolean reverse = false;

    @Option(names = { "-u", "--unique" })
    public boolean unique = false;

    @Option(names = { "-S", "--buffer-size" })
    public String bufferSize = null;

    @Option(names = { "-T", "--temporary-directory" })
    public String temporaryDirectory = null;

    // TODO Integrate oshi to get physical core count by default
    @Option(names = { "--parallel" })
    public int parallel = -1;

    // TODO Clarify merge semantics
    // At present it is for conflating consecutive named graphs with the same name
    // into a single graph
    @Option(names = { "-m", "--merge" })
    public boolean merge = false;

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.sort(this);
    }

}
