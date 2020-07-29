package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.sparql_integrate.ngs.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Count the number of graphs by default, or other aspects based on the
 * parameters
 *
 * @author raven
 *
 */
@Command(name = "wc", description = "Mimicks the wordcount (wc) command; counts graphs or quads")
public class CmdNgsWc implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    // TODO We should not use the name 'lines' when we mean triples/quads - maybe
    // 'elements' or 'items'?
    @Option(names = { "-l", "--lines" }, description = "Count triples/quads instead of graphs")
    public boolean numQuads = false;

    @Option(names = { "--nv", "--no-validate" }, description = "Only for nquads: Count lines instead of an actual parse")
    public boolean noValidate = false;

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.wc(this);
    }
}
