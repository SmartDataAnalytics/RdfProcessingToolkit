package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;

import picocli.CommandLine;


public class MainCliRdfProcessingToolkit {

    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) {
        int exitCode = mainCore(args);
        System.exit(exitCode);
    }

    public static int mainCore(String[] args) {
        int result = new CommandLine(new CmdRptMain())
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                ExceptionUtils.rethrowIfNotBrokenPipe(ex);
                return 0;
            })
            .execute(args);
        return result;
    }

}
