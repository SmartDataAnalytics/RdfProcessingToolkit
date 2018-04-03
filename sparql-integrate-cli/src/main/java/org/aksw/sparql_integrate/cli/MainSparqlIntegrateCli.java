package org.aksw.sparql_integrate.cli;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtIterator;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlStmtQuery;
import org.aksw.jena_sparql_api.update.FluentSparqlService;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.ext.com.google.common.io.CharStreams;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.out.SinkQuadOutput;
import org.apache.jena.riot.out.SinkTripleOutput;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class MainSparqlIntegrateCli {

	private static final Logger logger = LoggerFactory.getLogger(MainSparqlIntegrateCli.class);

	@Configuration
	public static class ConfigSparqlIntegrate {

		@Bean
		public ApplicationRunner applicationRunner() {
			return (args) -> {

				PrefixMapping pm = new PrefixMappingImpl();
				pm.setNsPrefixes(PrefixMapping.Extended);
				JenaExtensionUtil.addPrefixes(pm);

				JenaExtensionHttp.addPrefixes(pm);

				// System.out.println("ARGS: " + args.getOptionNames());
				Dataset dataset = DatasetFactory.create();
				RDFConnection conn = RDFConnectionFactory.connect(dataset);

				List<String> filenames = args.getNonOptionArgs();//args.getOptionValues("sparql");
				if (filenames == null || filenames.isEmpty()) {
					throw new RuntimeException(
							"No SPARQL files specified. Use one or more instances of the command line argument --sparql='filename'");
				}
				for (String filename : filenames) {
					File file = new File(filename).getAbsoluteFile();
					if(!file.exists()) {
						throw new FileNotFoundException(file.getAbsolutePath() + " does not exist");
					}
					
					String dirName = file.getParentFile().getAbsoluteFile().toURI().toString();

					Prologue prologue = new Prologue();
					prologue.setPrefixMapping(pm);

					prologue.setBaseURI(dirName);

					Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxARQ,
							prologue, true);// .getQueryParser();

					InputStream in = new FileInputStream(filename);
					Stream<SparqlStmt> stmts = parseSparqlQueryFile(in, sparqlStmtParser);

					stmts.forEach(stmt -> process(conn, stmt));
				}

				// Path path = Paths.get(args[0]);
				// //"/home/raven/Projects/Eclipse/trento-bike-racks/datasets/bikesharing/trento-bike-sharing.json");
				// String str = ""; //new String(Files.readAllBytes(path),
				// StandardCharsets.ISO_8859_1);
				// System.out.println(str);
				//

				// model.setNsPrefixes(PrefixMapping.Extended);
				// model.getResource(path.toAbsolutePath().toUri().toString()).addLiteral(RDFS.label,
				// str);

				SparqlService sparqlService = FluentSparqlService.from(conn).create();
				// QueryExecutionFactory qef = FluentQueryExecutionFactory.from(model)
				// .config()
				// //.withParser(sparqlStmtParser)
				// //.withPrefixes(PrefixMapping.Extended, false)
				// .end().create();

				if (args.containsOption("server")) {

					Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
							pm, false);// .getQueryParser();

					int port = 7532;
					Server server = FactoryBeanSparqlServer.newInstance()
							.setSparqlServiceFactory((serviceUri, datasetDescription, httpClient) -> sparqlService)
							.setSparqlStmtParser(sparqlStmtParser).setPort(port).create();

					server.start();

					URI browseUri = new URI("http://localhost:" + port + "/sparql");
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().browse(browseUri);
					} else {
						System.err.println("SPARQL service with in-memory result dataset running at " + browseUri);
					}

					server.join();
				}
			};
		}
	}

	public static void process(RDFConnection conn, SparqlStmt stmt) {
		logger.info("Processing SPARQL Statement: " + stmt);

		if (stmt.isQuery()) {
			SparqlStmtQuery qs = stmt.getAsQueryStmt();
			Query q = qs.getQuery();
			q.isConstructType();
			conn.begin(ReadWrite.READ);
			// SELECT -> STDERR, CONSTRUCT -> STDOUT
			QueryExecution qe = conn.query(q);

			if (q.isConstructQuad()) {
				// ResultSetFormatter.ntrqe.execConstructTriples();
				//throw new RuntimeException("not supported yet");
				SinkQuadOutput sink = new SinkQuadOutput(System.out, null, null);
				Iterator<Quad> it = qe.execConstructQuads();
				while (it.hasNext()) {
					Quad t = it.next();
					sink.send(t);
				}
				sink.flush();
				sink.close();

			} else if (q.isConstructType()) {
				// System.out.println(Algebra.compile(q));

				SinkTripleOutput sink = new SinkTripleOutput(System.out, null, null);
				Iterator<Triple> it = qe.execConstructTriples();
				while (it.hasNext()) {
					Triple t = it.next();
					sink.send(t);
				}
				sink.flush();
				sink.close();
			} else if (q.isSelectType()) {
				ResultSet rs = qe.execSelect();
				String str = ResultSetFormatter.asText(rs);
				System.err.println(str);
			} else {
				throw new RuntimeException("Unsupported query type");
			}

			conn.end();
		} else if (stmt.isUpdateRequest()) {
			UpdateRequest u = stmt.getAsUpdateStmt().getUpdateRequest();

			conn.update(u);
		}
	}

	public static void main(String[] args) {
		JenaExtensionHttp.register(() -> HttpClientBuilder.create().build());

		// RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create());
		// System.out.println(ResultSetFormatter.asText(conn.query("SELECT * {
		// BIND('test' AS ?s) }").execSelect()));
		// System.out.println(ResultSetFormatter.asText(conn.query("SELECT * { {}
		// BIND('test' AS ?s) }").execSelect()));

		try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder().sources(ConfigSparqlIntegrate.class)
				.bannerMode(Banner.Mode.OFF)
				// If true, Desktop.isDesktopSupported() will return false, meaning we can't
				// launch a browser
				.headless(false).web(false).run(args)) {

		}
	}

	public static Stream<SparqlStmt> parseSparqlQueryFile(InputStream in, Function<String, SparqlStmt> parser)
			throws IOException, ParseException {
		// try(QueryExecution qe = qef.createQueryExecution(q)) {
		// Model result = qe.execConstruct();
		// RDFDataMgr.write(System.out, result, RDFFormat.TURTLE_PRETTY);
		// //ResultSet rs = qe.execSelect();
		// //System.out.println(ResultSetFormatter.asText(rs));
		// }
		// File file = new
		// File("/home/raven/Projects/Eclipse/trento-bike-racks/datasets/test/test.sparql");
		// String str = Files.asCharSource(, StandardCharsets.UTF_8).read();

		String str = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));

		// ARQParser parser = new ARQParser(new FileInputStream(file));
		// parser.setQuery(new Query());
		// parser.

		// SparqlStmtParser parser = SparqlStmtParserImpl.create(Syntax.syntaxARQ,
		// PrefixMapping.Extended, true);

		Stream<SparqlStmt> result = Streams.stream(new SparqlStmtIterator(parser, str));
		return result;
	}

}
