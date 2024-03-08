package org.aksw.bench.geo.cmd;

import picocli.CommandLine.Command;

@Command(name="bench", description = "Quick Benchmark Toolkit", subcommands = {
    CmdBenchGenParent.class,
})
public class CmdBenchParent {

}
