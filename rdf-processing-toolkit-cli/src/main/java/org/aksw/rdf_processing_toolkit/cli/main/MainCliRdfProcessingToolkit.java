package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdUtils;

import picocli.CommandLine;

public class MainCliRdfProcessingToolkit {
    public static void main(String[] args) throws Exception {
        // CmdUtils.execCmd(CmdRptMain.class, args);
        CommandLine commandLine = new CommandLine(new CmdRptMain());
        
        // Register sansa dynamically
        CmdUtils.registerIfAvailable(commandLine, "net.sansa_stack.spark.cli.cmd.CmdSansaParent");

        CmdUtils.execCmd(commandLine, args);
    }
}
