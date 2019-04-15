package org.aksw.sparql_integrate.cli;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.sparql.ext.fs.JenaExtensionFs;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlStmtQuery;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.update.FluentSparqlService;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Quad;
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

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Stopwatch;

@SpringBootApplication
public class MainCliSparqlIntegrate {

	private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlIntegrate.class);

	
	
	public static Model parseTurtleAgainstModel(Model model, PrefixMapping prefixMapping, InputStream in) {
		// Add namespaces from the spec
		// Apparently Jena does not support parsing against
		// namespace prefixes previously declared in the target model
		// Therefore we serialize the prefix declarations and prepend them to the
		// input stream of the dataset		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Model tmp = ModelFactory.createDefaultModel();
		tmp.setNsPrefixes(prefixMapping);
		RDFDataMgr.write(baos, tmp, Lang.TURTLE);
		
		InputStream combined = new SequenceInputStream(
				new ByteArrayInputStream(baos.toByteArray()), in);
		
		RDFDataMgr.read(model, combined, Lang.TURTLE);
		
		return model;
	}

	@Configuration
	public static class ConfigSparqlIntegrate {

		@Bean
		public ApplicationRunner applicationRunner() {
			return (args) -> {
				
				Collection<RDFFormat> available = RDFWriterRegistry.registered();
				String optOutFormat = Optional.ofNullable(args.getOptionValues("w")).map(x -> x.iterator().next()).orElse(null);

				RDFFormat outFormat = null;
				if(optOutFormat != null) {
			        outFormat = available.stream()
			        		.filter(f -> f.toString().equalsIgnoreCase(optOutFormat))
			        		.findFirst()
							.orElseThrow(() -> new RuntimeException("Unknown format: " + optOutFormat + " Available: " + available));
				}
//				System.out.println(outFormat);
				
				Sink<Quad> sink = SparqlStmtUtils.createSink(outFormat, System.out);
				

				// TODO Replace with our RDFDataMgrEx 
				
				PrefixMapping pm = new PrefixMappingImpl();
				pm.setNsPrefixes(PrefixMapping.Extended);
				JenaExtensionUtil.addPrefixes(pm);

				JenaExtensionHttp.addPrefixes(pm);

				// Extended SERVICE <> keyword implementation
				JenaExtensionFs.registerFileServiceHandler();
				
				// System.out.println("ARGS: " + args.getOptionNames());
				Dataset dataset = DatasetFactory.create();
				RDFConnection conn = RDFConnectionFactory.connect(dataset);

				List<String> filenames = args.getNonOptionArgs();//args.getOptionValues("sparql");
				if (filenames == null || filenames.isEmpty()) {
					throw new RuntimeException(
							"No SPARQL files specified.");
				}
				
				Stopwatch sw = Stopwatch.createStarted();

				Path cwd = null;

				String cwdKey = "cwd=";
				String cwdResetCwd = "cwd";

				for (String filename : filenames) {
					logger.info("Processing item " + filename);
					// Determine the current working directory
//					System.out.println(filename);
//					System.out.println("foo: " + SparqlStmtUtils.extractBaseIri(filename));

					// Just use class path resources for pre-configured queries
//					if(filename.equals("emit")) {
//						processSparqlStmtWrapper(conn, new SparqlStmtQuery("CONSTRUCT WHERE { ?s ?p ?o }"), sink::send);
//					} else 
					if(filename.startsWith(cwdKey)) {
						String cwdValue = filename.substring(cwdKey.length()).trim();

						if(cwd == null) {
							cwd = Paths.get(StandardSystemProperty.USER_DIR.value());
						}
						
						cwd = cwd.resolve(cwdValue);
						logger.info("Pinned working directory to " + cwd);
					} else if(filename.equals(cwdResetCwd)) {
						// If cwdValue is an empty string, reset the working directory
						logger.info("Unpinned working directory");

						cwd = null;
					} else {

						Lang rdfLang = RDFDataMgr.determineLang(filename, null, null);
						if(rdfLang != null) {
							
							Model tmp = ModelFactory.createDefaultModel();
							InputStream in = SparqlStmtUtils.openInputStream(filename);
							// FIXME Validate we are really using turtle here
							parseTurtleAgainstModel(tmp, pm, in);

//							tmp.setNsPrefixes(pm);
//							RDFDataMgr.read(tmp, filename);
							
							// FIXME control which graph to load into - by default its the default graph
							logger.info("RDF File detected, loading into graph");
							conn.load(tmp);
						} else {
							
							String baseIri = cwd == null ? null : cwd.toUri().toString();
							SparqlStmtUtils.processFile(pm, filename, baseIri)
								.forEach(stmt -> processSparqlStmtWrapper(conn, stmt, sink::send));
						}
					}
				}
				
				sink.flush();
				sink.close();

				
				logger.info("SPARQL overall execution finished after " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");
				
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

	public static void processSparqlStmtWrapper(RDFConnection conn, SparqlStmt stmt, Consumer<Quad> sink) {
		Stopwatch sw2 = Stopwatch.createStarted();
		processSparqlStmt(conn, stmt, sink);
		logger.info("SPARQL stmt execution finished after " + sw2.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");
	}

	public static void processSparqlStmt(RDFConnection conn, SparqlStmt stmt, Consumer<Quad> sink) {
		logger.info("Processing SPARQL Statement: " + stmt);
		if(stmt.isQuery()) {
			Query q = stmt.getAsQueryStmt().getQuery();
			Op op = Algebra.compile(q);
			logger.info("Algebra: " + op);
		}
		SparqlStmtUtils.process(conn, stmt, sink);
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

}
