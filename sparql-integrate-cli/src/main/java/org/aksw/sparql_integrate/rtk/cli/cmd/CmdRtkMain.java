package org.aksw.sparql_integrate.rtk.cli.cmd;

import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsMain;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="rtk", description = "Subcommands", subcommands = {
        CmdNgsMain.class,
        CmdSparqlIntegrateMain.class
})
public class CmdRtkMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;


}
