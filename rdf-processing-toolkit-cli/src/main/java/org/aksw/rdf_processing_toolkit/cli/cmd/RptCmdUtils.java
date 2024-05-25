package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.commons.picocli.CmdUtils;
import org.aksw.sparql_integrate.cli.CmdUtilsBackport;

/**
 * Wrapper for {@link CmdUtils} that initializes global settings.
 */
public class RptCmdUtils {
    static { CliUtils.configureGlobalSettings(); }

    public static void execCmd(Class<?> cmdClass, String[] args) {
        CmdUtilsBackport.execCmd(cmdClass, args);
    }
}
