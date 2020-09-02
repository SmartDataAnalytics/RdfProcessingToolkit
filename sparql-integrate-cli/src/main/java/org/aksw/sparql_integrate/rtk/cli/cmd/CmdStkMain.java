package org.aksw.sparql_integrate.rtk.cli.cmd;

import org.aksw.ngs.cli.cmd.CmdNgsMain;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="stk", description = "SPARQL Toolkit", subcommands = {
        CmdNgsMain.class,
        CmdSparqlIntegrateMain.class
})
public class CmdStkMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;


}
