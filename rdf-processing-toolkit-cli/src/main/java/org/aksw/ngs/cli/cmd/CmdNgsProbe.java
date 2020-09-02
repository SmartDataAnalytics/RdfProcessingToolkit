package org.aksw.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.ngs.cli.main.NgsCmdImpls;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Probe the RDF language of some input by trying out all available parsers
 *
 * @author raven
 *
 */
@Command(name = "probe", description = "Determine content type based on input")
public class CmdNgsProbe implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    /**
     * sparql-pattern file
     *
     */
//	@Parameter(names={"-n"}, description="numRecords")
//	public long numRecords = 10;

//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return NgsCmdImpls.probe(this);
    }

}
