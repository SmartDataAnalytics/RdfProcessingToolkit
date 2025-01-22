package org.aksw.rdf_processing_toolkit.cli.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.sparql.core.DatasetDescription;

import picocli.CommandLine.Option;

public class CmdMixinSparqlDataset
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Option(names = { "--dg", "--default-graph" }, description="Default graph")
    public List<String> defaultGraphs = new ArrayList<>();

    @Option(names = { "--ng", "--named-graph" }, description="Named graph")
    public List<String> namedGraphs = new ArrayList<>();

    @Option(names = { "--service" }, description = "SPARQL endpoint URL")
    public boolean serviceUrl;

    public static DatasetDescription toDatasetDescription(CmdMixinSparqlDataset cmd) {
        return new DatasetDescription(cmd.defaultGraphs, cmd.namedGraphs);
    }
}
