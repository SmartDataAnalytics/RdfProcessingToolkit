package org.aksw.named_graph_stream.cli.cmd;

import org.aksw.rdf_processing_toolkit.cli.cmd.CmdCommonBase;
import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;

import picocli.CommandLine.Command;

@Command(name="ngs",
            versionProvider = VersionProviderRdfProcessingToolkit.class,
            description = "Named Graph Stream Subcommands",
            subcommands = {
        CmdNgsCat.class,
        CmdNgsFilter.class,
        CmdNgsHead.class,
        CmdNgsTail.class,
        CmdNgsMap.class,
//        CmdNgsMerge.class,
        CmdNgsProbe.class,
        CmdNgsSort.class,
        CmdNgsSubjects.class,
        CmdNgsUntil.class,
        CmdNgsWc.class,
        CmdNgsWhile.class,
        CmdNgsGit.class
})
public class CmdNgsMain
	extends CmdCommonBase
{
}
