package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.commons.picocli.CmdUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;

import picocli.CommandLine;

public class MainCliRdfProcessingToolkit {
    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) throws Exception {
        // CmdUtils.execCmd(CmdRptMain.class, args);
        CommandLine commandLine = new CommandLine(new CmdRptMain());

        // Register sansa dynamically
        CmdUtils.registerIfAvailable(commandLine, "net.sansa_stack.spark.cli.cmd.CmdSansaParent");

        CmdUtils.execCmd(commandLine, args);
    }
}
