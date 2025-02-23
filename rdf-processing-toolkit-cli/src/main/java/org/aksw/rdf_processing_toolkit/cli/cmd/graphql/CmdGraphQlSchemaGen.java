package org.aksw.rdf_processing_toolkit.cli.cmd.graphql;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.commons.io.util.StdIo;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor;
import org.aksw.jenax.dataaccess.sparql.datasource.RDFDataSource;
import org.aksw.jenax.dataaccess.sparql.factory.datasource.RDFDataSources;
import org.aksw.jenax.graphql.schema.generator.GraphQlSchemaGenerator;
import org.aksw.jenax.graphql.schema.generator.GraphQlSchemaGenerator.TypeInfo;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.jenax.stmt.util.SparqlStmtUtils;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdMixinSparqlDataset;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.RDFConnection;

import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.parser.Parser;
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

    @Parameters(arity = "0..*", description = "Input files")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        SparqlScriptProcessor processor = SparqlScriptProcessor.createWithEnvSubstitution(null);
        processor.process(nonOptionArgs);

        Dataset dataset = DatasetFactory.create();
        try (RDFConnection conn = RDFConnection.connect(dataset)) {
            for (SparqlStmt stmt : processor.getPlainSparqlStmts()) {
                SparqlStmtUtils.execAny(conn, stmt, null);
            }
        }

        RDFDataSource dataSource = RDFDataSources.of(dataset);
        List<TypeInfo> types = GraphQlSchemaGenerator.summarize(dataSource);

        GraphQlSchemaGenerator generator = new GraphQlSchemaGenerator();
        Document doc = generator.process(types);
        String str = AstPrinter.printAst(doc);

        try (Writer writer = new OutputStreamWriter(StdIo.openStdOutWithCloseShield())) {
            writer.write(str);
        }

        boolean validateOutput = true;
        if (validateOutput) {
            Parser parser = new Parser();
            parser.parse(str);
        }

        return 0;
    }
}
