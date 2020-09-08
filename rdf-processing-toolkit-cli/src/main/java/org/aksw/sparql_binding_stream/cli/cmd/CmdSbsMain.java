package org.aksw.sparql_binding_stream.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="sbs", description = "SPARQL Binding Streams Subcommands", subcommands = {
        CmdSbsMap.class,
        CmdSbsFilter.class
})
public class CmdSbsMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

//    @Parameter(names={"-o", "--out-format"})
//    public String format = "trig/pretty";

}