package org.aksw.sparql_integrate.cli;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
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
import org.apache.jena.query.QueryParseException;
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

import com.google.common.base.Strings;
import com.google.common.collect.AbstractIterator;

class SparqlStmtIterator extends AbstractIterator<SparqlStmt> {

	protected Function<String, SparqlStmt> parser;

	protected String str;
	protected int line = 1;
	protected int column = 1;

	public SparqlStmtIterator(Function<String, SparqlStmt> parser, String str) {
		super();
		this.parser = parser;
		this.str = str;
	}

	public static int toCharPos(String str, int lineNumber, int columnNumber) {
		BufferedReader br = new BufferedReader(new StringReader(str));

		int lineIndex = Math.max(0, lineNumber - 1);
		int columnIndex = Math.max(0, columnNumber - 1);

		int result = 0;
		for (int i = 0; i < lineIndex; ++i) {
			String l;
			try {
				l = br.readLine();
			} catch (IOException e) {
				// Should never happen
				throw new RuntimeException(e);
			}
			result = result + l.length() + 1; // +1 -> the newline character
		}

		result += columnIndex;

		return result;
	}

	public static boolean isEmptyString(String str) {
		return Strings.isNullOrEmpty(str.trim());
	}

	// public static raiseException(QueryParseException ex) {
	//
	// }

	public static Pattern posPattern = Pattern.compile("line (\\d+), column (\\d+)");

	public static int[] parsePos(String str) {
		Matcher m = posPattern.matcher(str);

		int[] result = m.find() ? new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) }
				: new int[] { 0, 0 };

		return result;
	}

	@Override
	protected SparqlStmt computeNext() {
		if (isEmptyString(str)) {
			return endOfData();
		}

		SparqlStmt result = parser.apply(str);

		// Get the string up to the point where a parse error was encountered
		QueryParseException ex = result.getParseException();
		if (ex != null) {
			int[] exPos = parsePos(ex.getMessage());

			int pos = toCharPos(str, exPos[0], exPos[1]);

			line = line + Math.max(0, exPos[0] - 1);
			column = column + Math.max(0, exPos[1] - 1);

			String retryStr = str.substring(0, pos);

			// Note: Jena parses an empty string as a sparql update statement without errors
			if (isEmptyString(retryStr)) {
				throw new RuntimeException("Error near line " + line + ", column " + column + ".", ex);
			}

			result = parser.apply(retryStr);

			QueryParseException retryEx = result.getParseException();
			if (retryEx != null) {
				throw new RuntimeException("Error near line " + line + ", column " + column + ".", ex);
			}

			str = str.substring(pos);
		} else {
			// TODO Move position to last char in the string
			str = "";
		}

		return result;
	}
}

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

				List<String> filenames = args.getOptionValues("sparql");
				if (filenames == null) {
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
