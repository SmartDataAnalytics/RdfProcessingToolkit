package org.aksw.sparql_integrate.rtk.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.sparql_integrate.rtk.cli.cmd.CmdRtkMain;

import picocli.CommandLine;


public class MainCliRdfToolkit {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CmdRtkMain())
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    ExceptionUtils.rethrowIfNotBrokenPipe(ex);
                    return 0;
                })
                .execute(args);

        System.exit(exitCode);
    }
}
