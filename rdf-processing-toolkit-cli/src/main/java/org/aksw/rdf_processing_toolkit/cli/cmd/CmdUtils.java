package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.commons.util.exception.ExceptionUtilsAksw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

/**
 * callCmd: Run command and return the exit code
 * execCmd: Run command and terminate the JVM using System.exit with the exit code
 *
 * @author raven
 *
 */
public class CmdUtils {
    static { CliUtils.configureGlobalSettings(); }
    private static final Logger logger = LoggerFactory.getLogger(CmdUtils.class);

    public static void execCmd(Class<?> cmdClass, String[] args) {
        int exitCode = callCmd(cmdClass, args);
        System.exit(exitCode);
    }

    public static void execCmd(Object cmdInstance, String[] args) {
        int exitCode = callCmd(cmdInstance, args);
        System.exit(exitCode);
    }

    public static void execCmd(CommandLine cl, String[] args) {
        int exitCode = callCmd(cl, args);
        System.exit(exitCode);
    }

    /**
     *
     * @param args The application arguments
     * @param cmdClass Command class with no arg ctor
     * @return
     */
    public static int callCmd(Class<?> cmdClass, String[] args) {
        try {
            Object cmd = cmdClass.getDeclaredConstructor().newInstance();
            int result = callCmd(cmd, args);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method to launch a command with a generally useful exception handling
     * configuration:
     * If the command supports HasDebugMode then display of exception is as follows:
     * on: the full stack trace is shown
     * off: only the root cause message (without the stack trace) of an exception is shown
     *
     * If the debug flag is not supported then the output matches the debug=on case (full stack trace is shown).
     *
     *
     * @param args
     * @param cmdInstance
     * @return
     */
    public static int callCmd(Object cmdInstance, String[] args) {
        int result = callCmd(new CommandLine(cmdInstance), args);
        return result;
    }

    public static int callCmd(CommandLine cl, String[] args) {
        int result = cl
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                Object cmd = commandLine.getCommand();
                boolean debugMode = cmd instanceof HasDebugMode
                        ? ((HasDebugMode)cmd).isDebugMode()
                        : true;

                if (debugMode) {
                    ExceptionUtilsAksw.rethrowIfNotBrokenPipe(ex);
                } else {
                    ExceptionUtilsAksw.forwardRootCauseMessageUnless(ex, logger::error, ExceptionUtilsAksw::isBrokenPipeException);
                    return 1;
                }
                return 0;
            })
            .execute(args);
        return result;
    }

    public static CommandLine registerIfAvailable(CommandLine commandLine, String className) {
        CommandLine result = null;
        try {
            Class<?> cmdCls = Class.forName(className);
            Object cmd;
            try {
                cmd = cmdCls.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            result = commandLine.addSubcommand(cmd);
        } catch (ClassNotFoundException e) {
            // Ignore class no found (XXX Add logging?)
        }

        return result;
    }
}
