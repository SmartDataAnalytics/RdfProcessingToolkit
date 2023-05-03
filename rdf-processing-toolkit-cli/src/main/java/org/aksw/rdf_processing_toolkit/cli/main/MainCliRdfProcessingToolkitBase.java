package org.aksw.rdf_processing_toolkit.cli.main;

//
//public abstract class MainCliRdfProcessingToolkitBase {
//    private static final Logger logger = LoggerFactory.getLogger(MainCliRdfProcessingToolkitBase.class);
//
//    static { CliUtils.configureGlobalSettings(); }
//
//    protected abstract Object getCommand();
//
//    public static void main(String[] args) {
//        int exitCode = mainCore(args);
//        System.exit(exitCode);
//    }
//
//    public static int mainCore(String[] args) {
//        Object cmd = getCommand();
//
//        int result = new CommandLine(cmd)
//            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
//                boolean debugMode = false;
//                if (debugMode) {
//                    ExceptionUtils.rethrowIfNotBrokenPipe(ex);
//                } else {
//                    ExceptionUtils.forwardRootCauseMessageUnless(ex, logger::error, ExceptionUtils::isBrokenPipeException);
//                }
//                return 0;
//            })
//            .execute(args);
//        return result;
//    }
//
//}
