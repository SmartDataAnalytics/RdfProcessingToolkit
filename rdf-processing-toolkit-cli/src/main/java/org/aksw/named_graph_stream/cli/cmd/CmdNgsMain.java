package org.aksw.named_graph_stream.cli.cmd;

import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
public class CmdNgsMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "-v", "--version" }, versionHelp = true)
    public boolean version = false;

//    @Parameter(names={"-o", "--out-format"})
//    public String format = "trig/pretty";

}
