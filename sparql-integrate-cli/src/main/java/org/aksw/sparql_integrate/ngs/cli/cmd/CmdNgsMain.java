package org.aksw.sparql_integrate.ngs.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="ngs", description = "Named Graph Stream Subcommands", subcommands = {
        CmdNgsCat.class,
        CmdNgsFilter.class,
        CmdNgsHead.class,
        CmdNgsMap.class,
//        CmdNgsMerge.class,
        CmdNgsProbe.class,
        CmdNgsSort.class,
        CmdNgsSubjects.class,
        CmdNgsUntil.class,
        CmdNgsWc.class,
        CmdNgsWhile.class
})
public class CmdNgsMain {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

//    @Parameter(names={"-o", "--out-format"})
//    public String format = "trig/pretty";

}
