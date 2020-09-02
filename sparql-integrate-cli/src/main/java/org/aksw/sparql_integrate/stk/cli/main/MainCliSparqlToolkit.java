package org.aksw.sparql_integrate.stk.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.sparql_integrate.rtk.cli.cmd.CmdStkMain;

import picocli.CommandLine;


public class MainCliSparqlToolkit {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CmdStkMain())
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    ExceptionUtils.rethrowIfNotBrokenPipe(ex);
                    return 0;
                })
                .execute(args);

        System.exit(exitCode);
    }
}
