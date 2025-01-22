package org.aksw.rdf_processing_toolkit.cli.cmd.graphql;

import org.aksw.rml.cli.cmd.VersionProviderRmlTk;

import picocli.CommandLine.Command;

@Command(name="graphqltk", versionProvider = VersionProviderRmlTk.class, description = "GraphQl Toolkit", subcommands = {
    CmdGraphQlSchemaGen.class
})
public class CmdGraphQlTkParent {
}
