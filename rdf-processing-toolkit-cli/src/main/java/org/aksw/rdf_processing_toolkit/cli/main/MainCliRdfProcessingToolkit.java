package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.aksw.sparql_integrate.cli.CmdUtilsBackport;

import picocli.CommandLine;

public class MainCliRdfProcessingToolkit {
    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) throws Exception {
        // CmdUtils.execCmd(CmdRptMain.class, args);
        CommandLine commandLine = new CommandLine(new CmdRptMain());

        // Register sansa dynamically
        CmdUtilsBackport.registerIfAvailable(commandLine, "net.sansa_stack.spark.cli.cmd.CmdSansaParent");

        CmdUtilsBackport.execCmd(commandLine, args);
    }
}
