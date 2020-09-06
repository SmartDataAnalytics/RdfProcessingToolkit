package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.sparql_integrate.cli.MainCliSparqlIntegrateOld;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// Old cmd / remove it!
@Command(name="integrate", description = "Sparql Integrate")
public class CmdSparqlIntegrateMain
    implements Runnable
{
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Parameters(hidden = true) String[] all;

    @Override
    public void run() {
        MainCliSparqlIntegrateOld.main(all);
    }
}
