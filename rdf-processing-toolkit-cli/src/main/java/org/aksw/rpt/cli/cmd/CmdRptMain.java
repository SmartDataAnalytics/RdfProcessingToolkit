package org.aksw.rpt.cli.cmd;

import org.aksw.ngs.cli.cmd.CmdNgsMain;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="rpt", description = "RDF Processing Toolkit", subcommands = {
        CmdNgsMain.class,
        CmdSparqlIntegrateMain.class
})
public class CmdRptMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;


}
