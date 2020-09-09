package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.commons.util.exception.ExceptionUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;


public class MainCliRdfProcessingToolkit {
    private static final Logger logger = LoggerFactory.getLogger(MainCliRdfProcessingToolkit.class);

    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) {
        int exitCode = mainCore(args);
        System.exit(exitCode);
    }

    public static int mainCore(String[] args) {
        int result = new CommandLine(new CmdRptMain())
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                ExceptionUtils.forwardRootCauseMessageUnless(ex, logger::error, ExceptionUtils::isBrokenPipeException);
                return 0;
            })
            .execute(args);
        return result;
    }

}
