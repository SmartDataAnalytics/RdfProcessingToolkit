package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.named_graph_stream.cli.cmd.CmdNgsMain;

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
