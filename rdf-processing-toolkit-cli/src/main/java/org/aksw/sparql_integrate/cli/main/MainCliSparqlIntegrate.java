package org.aksw.sparql_integrate.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;

import picocli.CommandLine;

public class MainCliSparqlIntegrate {
    public static void main(String[] args) {
        int exitCode = mainCore(args);
        System.exit(exitCode);
    }

    public static int mainCore(String[] args) {
        int result = new CommandLine(new CmdSparqlIntegrateMain())
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                ExceptionUtils.rethrowIfNotBrokenPipe(ex);
                return 0;
            })
            .execute(args);
        return result;
    }

}
