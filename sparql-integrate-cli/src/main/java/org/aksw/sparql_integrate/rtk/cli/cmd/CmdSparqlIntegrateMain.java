package org.aksw.sparql_integrate.rtk.cli.cmd;

import org.aksw.sparql_integrate.cli.MainCliSparqlIntegrate;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name="integrate", description = "Sparql Integrate")
public class CmdSparqlIntegrateMain
    implements Runnable
{
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Parameters(hidden = true) String[] all;

    @Override
    public void run() {
        MainCliSparqlIntegrate.main(all);
    }
}
