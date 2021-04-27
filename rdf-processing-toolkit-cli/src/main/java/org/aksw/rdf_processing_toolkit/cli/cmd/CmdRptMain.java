package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.named_graph_stream.cli.cmd.CmdNgsMain;
import org.aksw.sparql_binding_stream.cli.cmd.CmdSbsMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;

import picocli.CommandLine.Command;

@Command(name="rpt", versionProvider = VersionProviderRdfProcessingToolkit.class, description = "RDF Processing Toolkit", subcommands = {
        CmdNgsMain.class,
        CmdSparqlIntegrateMain.class,
        CmdSbsMain.class
})
public class CmdRptMain
	extends CmdCommonBase
{
}
