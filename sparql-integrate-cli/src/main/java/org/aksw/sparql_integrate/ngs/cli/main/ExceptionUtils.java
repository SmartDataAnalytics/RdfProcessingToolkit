package org.aksw.sparql_integrate.ngs.cli.main;

public class ExceptionUtils {
    public static boolean isBrokenPipeException(Throwable t) {
        String str = org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage(t);
        boolean result = str.toLowerCase().contains("broken pipe");
        return result;
    }

    public static void rethrowIfNotBrokenPipe(Throwable t) {
        if(isBrokenPipeException(t)) {
            // Silently ignore
        } else {
            throw new RuntimeException(t);
        }
    }
}
