package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdUtils;

public class MainCliRdfProcessingToolkit {

    public static void main(String[] args) {
        CmdUtils.execCmd(CmdRptMain.class, args);
    }

}
