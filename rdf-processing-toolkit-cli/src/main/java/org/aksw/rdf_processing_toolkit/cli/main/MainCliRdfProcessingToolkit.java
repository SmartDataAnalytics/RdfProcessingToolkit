package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdUtils;
import org.apache.jena.riot.RDFLanguages;


public class MainCliRdfProcessingToolkit {
    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) {
        CmdUtils.execCmd(CmdRptMain.class, args);
    }

}
