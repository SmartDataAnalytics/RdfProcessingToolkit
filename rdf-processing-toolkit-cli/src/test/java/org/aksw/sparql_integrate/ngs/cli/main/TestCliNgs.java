package org.aksw.sparql_integrate.ngs.cli.main;

import org.aksw.named_graph_stream.cli.main.MainCliNamedGraphStream;
import org.junit.Test;

public class TestCliNgs {

    @Test
    public void test() {
//        MainCliNamedGraphStream.mainCore(new String[] {"tail", "-n", "+1", "ngs-nato-phonetic-alphabet.trig"});
//        MainCliNamedGraphStream.mainCore(new String[] {"map", "--sparql", "* { ?s foaf:name ?o }", "ngs-nato-phonetic-alphabet.trig"});
        MainCliNamedGraphStream.mainCore(new String[] {"map", "--sparql", "CONSTRUCT WHERE { ?s foaf:name ?o }", "ngs-nato-phonetic-alphabet.trig"});
    }

}
