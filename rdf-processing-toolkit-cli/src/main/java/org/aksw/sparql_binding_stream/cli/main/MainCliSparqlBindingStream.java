package org.aksw.sparql_binding_stream.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdUtils;
import org.aksw.sparql_binding_stream.cli.cmd.CmdSbsMain;

public class MainCliSparqlBindingStream {
    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) {
    	CmdUtils.execCmd(CmdSbsMain.class, args); 
    }
}
