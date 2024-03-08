package org.aksw.bench.geo.cmd;

import picocli.CommandLine.Command;

@Command(name="grid", description = "Grid-based spatial benchmark generator", subcommands = {
    CmdBenchGenGridDataGen.class,
    CmdBenchGenGridQueryGen.class
})
public class CmdBenchGenGridParent {
}
