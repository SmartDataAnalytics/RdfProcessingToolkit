package org.aksw.rdf_processing_toolkit.cli.main;

import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdUtils;
import org.apache.jena.graph.NodeFactory;

public class MainCliRdfProcessingToolkit {
    public static void main(String[] args) {
        System.out.println(NodeFactory.createURI("http://foo.bar/baz").toString());
        CmdUtils.execCmd(CmdRptMain.class, args);
    }
}
