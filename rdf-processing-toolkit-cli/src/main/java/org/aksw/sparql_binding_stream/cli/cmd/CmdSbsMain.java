package org.aksw.sparql_binding_stream.cli.cmd;

import org.aksw.rdf_processing_toolkit.cli.cmd.CmdCommonBase;
import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;

import picocli.CommandLine.Command;

@Command(name="sbs",
    versionProvider = VersionProviderRdfProcessingToolkit.class,
    description = "SPARQL Binding Streams Subcommands", subcommands = {
        CmdSbsMap.class,
        CmdSbsFilter.class
})
public class CmdSbsMain
	extends CmdCommonBase
{
}