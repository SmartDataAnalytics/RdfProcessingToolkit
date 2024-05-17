package org.aksw.sparql_integrate.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.RptCmdUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;

public class MainCliSparqlIntegrate {
    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) {
        RptCmdUtils.execCmd(CmdSparqlIntegrateMain.class, args);
    }
}
