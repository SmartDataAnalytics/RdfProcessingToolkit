package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.commons.picocli.CmdUtils;

/**
 * Wrapper for {@link CmdUtils} that initializes global settings.
 */
public class RptCmdUtils {
    static { CliUtils.configureGlobalSettings(); }

    public static void execCmd(Class<?> cmdClass, String[] args) {
        CmdUtils.execCmd(cmdClass, args);
    }
}
