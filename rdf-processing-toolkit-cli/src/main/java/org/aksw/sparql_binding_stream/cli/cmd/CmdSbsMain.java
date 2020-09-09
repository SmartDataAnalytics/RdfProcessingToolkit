package org.aksw.sparql_binding_stream.cli.cmd;

import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="sbs",
    versionProvider = VersionProviderRdfProcessingToolkit.class,
    description = "SPARQL Binding Streams Subcommands", subcommands = {
        CmdSbsMap.class,
        CmdSbsFilter.class
})
public class CmdSbsMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "-v", "--version" }, versionHelp = true)
    public boolean version = false;

//    @Parameter(names={"-o", "--out-format"})
//    public String format = "trig/pretty";

}