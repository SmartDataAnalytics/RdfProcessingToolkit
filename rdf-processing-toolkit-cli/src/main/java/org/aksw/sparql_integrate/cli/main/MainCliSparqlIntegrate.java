package org.aksw.sparql_integrate.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class MainCliSparqlIntegrate {
    private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlIntegrate.class);

    public static void main(String[] args) {
        int exitCode = mainCore(args);
        System.exit(exitCode);
    }

    public static int mainCore(String[] args) {
        int result = new CommandLine(new CmdSparqlIntegrateMain())
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                ExceptionUtils.forwardRootCauseMessageUnless(ex, logger::error, ExceptionUtils::isBrokenPipeException);
                return 0;
            })
            .execute(args);
        return result;
    }

}
