package org.aksw.bench.geo.cmd;

import picocli.CommandLine.Command;

@Command(name="gen", description = "Benchmark generation tools", subcommands = {
    CmdBenchGenGridParent.class,
})
public class CmdBenchGenParent {

}
