package org.aksw.rdf_processing_toolkit.cli.cmd.graphql;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

import org.aksw.commons.io.util.StdIo;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor;
import org.aksw.jenax.graphql.schema.generator.GraphQlSchemaGenerator;
import org.aksw.jenax.graphql.schema.generator.GraphQlSchemaGenerator.TypeInfo;
import org.aksw.jenax.graphql.schema.generator.GraphQlSchemaSummarizer;
import org.aksw.jenax.graphql.util.GraphQlUtils;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdMixinSparqlDataset;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;

import graphql.language.AstPrinter;
import graphql.language.Document;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "schemagen", description = "Generate a schema GraphQL Schema over RDF data in files or in a SPARQL endpoinst.")
public class CmdGraphQlSchemaGen
    implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Mixin
    public CmdMixinSparqlDataset sparqlDataset = new CmdMixinSparqlDataset();

    @Option(names = { "-l", "--label-source" }, description = "An RDF dataset with labels for the classes and properties. Local names will be used as fallback.")
    public String labelSource;

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        Graph labelGraph = labelSource == null
            ? null
            : RDFDataMgr.loadGraph(labelSource);

        SparqlScriptProcessor processor = SparqlScriptProcessor.createWithEnvSubstitution(null);
        processor.process(nonOptionArgs);

        Dataset dataset = DatasetFactory.create();
        try (RDFConnection conn = RDFConnection.connect(dataset)) {
            for (SparqlStmt stmt : processor.getPlainSparqlStmts()) {
                SparqlStmtUtils.execAny(conn, stmt, null);
            }
        }

        // RDFDataSource dataSource = RDFDataSources.of(dataset);
        Graph graph = dataset.asDatasetGraph().getDefaultGraph(); // XXX Make configurable.
        List<TypeInfo> types = GraphQlSchemaSummarizer.summarize(graph);

        Function<String, String> iriToLabel = labelGraph == null
            ? null
            : iriStr -> {
                try (Stream<String> stream = labelGraph.stream(
                    NodeFactory.createURI(iriStr), RDFS.label.asNode(), Node.ANY)
                .map(Triple::getObject)
                .filter(Node::isLiteral)
                .map(Node::getLiteralLexicalForm)) {
                    return stream.findFirst().orElse(null);
                }
            };

        GraphQlSchemaGenerator generator = new GraphQlSchemaGenerator(iriToLabel);
        Document doc = generator.process(types);
        String str = AstPrinter.printAst(doc);

        try (Writer writer = new OutputStreamWriter(StdIo.openStdOutWithCloseShield())) {
            writer.write(str);
        }

        boolean validateOutput = true;
        if (validateOutput) {
            @SuppressWarnings("unused")
            Document reparsedDoc = GraphQlUtils.parseUnrestricted(str);
        }

        return 0;
    }
}
