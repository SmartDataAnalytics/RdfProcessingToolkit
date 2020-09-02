package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;

import picocli.CommandLine;


public class MainCliRdfProcessingToolkit {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CmdRptMain())
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    ExceptionUtils.rethrowIfNotBrokenPipe(ex);
                    return 0;
                })
                .execute(args);

        System.exit(exitCode);
    }
}
