package org.aksw.sparql_integrate.cli;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.core.RDFConnectionFactoryEx;
import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.json.RdfJsonUtils;
import org.aksw.jena_sparql_api.json.SPARQLResultVisitorSelectJsonOutput;
import org.aksw.jena_sparql_api.rx.RDFDataMgrEx;
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer;
import org.aksw.jena_sparql_api.sparql.ext.fs.JenaExtensionFs;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSink;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSinkQuads;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtIterator;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParser;
import org.aksw.jena_sparql_api.stmt.SparqlStmtParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.stmt.SparqlUpdateParser;
import org.aksw.jena_sparql_api.stmt.SparqlUpdateParserImpl;
import org.aksw.jena_sparql_api.update.FluentSparqlService;
import org.aksw.jena_sparql_api.utils.NodeUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.atlas.io.NullOutputStream;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.ext.com.google.common.base.Stopwatch;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.arq.ParseException;
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

import com.github.jsonldjava.shaded.com.google.common.collect.Iterators;
import com.google.common.base.StandardSystemProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

@SpringBootApplication
public class MainCliSparqlIntegrate {

	private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlIntegrate.class);

	
	public static TypedInputStream prependWithPrefixes(TypedInputStream in, PrefixMapping prefixMapping) {
		InputStream combined = prependWithPrefixes(in.getInputStream(), prefixMapping);
		
		TypedInputStream result = new TypedInputStream(combined, in.getMediaType(), in.getBaseURI());
		return result;
		
	}

	public static InputStream prependWithPrefixes(InputStream in, PrefixMapping prefixMapping) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Model tmp = ModelFactory.createDefaultModel();
		tmp.setNsPrefixes(prefixMapping);
		RDFDataMgr.write(baos, tmp, Lang.TURTLE);
//		System.out.println("Prefix str: " + baos.toString());
		
		InputStream combined = new SequenceInputStream(
				new ByteArrayInputStream(baos.toByteArray()), in);

		return combined;
	}
	
	public static Dataset parseTrigAgainstDataset(Dataset dataset, PrefixMapping prefixMapping, InputStream in) {
		// Add namespaces from the spec
		// Apparently Jena does not support parsing against
		// namespace prefixes previously declared in the target model
		// Therefore we serialize the prefix declarations and prepend them to the
		// input stream of the dataset		
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		Model tmp = ModelFactory.createDefaultModel();
//		tmp.setNsPrefixes(prefixMapping);
//		RDFDataMgr.write(baos, tmp, Lang.TURTLE);
////		System.out.println("Prefix str: " + baos.toString());
//		
//		InputStream combined = new SequenceInputStream(
//				new ByteArrayInputStream(baos.toByteArray()), in);
//		
		InputStream combined = prependWithPrefixes(in, prefixMapping);
		RDFDataMgr.read(dataset, combined, Lang.TRIG);
		
		return dataset;
	}

	
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
	
	public static final String cwdKey = "cwd=";
	public static final String cwdResetCwd = "cwd";

	
	@Configuration
	public static class ConfigSparqlIntegrate {

		@Bean
		public ApplicationRunner applicationRunner() {
			return args -> {

				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				
				if(false)
				{
					Model model = RDFDataMgr.loadModel("/home/raven/Projects/limbo/git/claus-playground-dataset/04-dcat-sparql/target/release.dcat.ttl");
					//RDFDataMgr.write(System.out, model, RDFFormat.JSONLD);
					
					try(QueryExecution qe = QueryExecutionFactory.create("SELECT ?ds ?dist { ?ds <http://www.w3.org/ns/dcat#distribution> ?dist }", model)) {
						ResultSet rs = qe.execSelect();
						JsonArray jsonElement = RdfJsonUtils.toJson(rs, 3, false);
//						while(rs.hasNext()) {
//							JsonElement jsonElement = toJson(rs.next().get("s"), 3);
//							String str = gson.toJson(jsonElement);
//							
//							System.out.println(str);
//						}
						String str = gson.toJson(jsonElement);
						
						System.out.println(str);

						//String str = ResultSetFormatter..asText(rs);
						//System.out.println(str);
//						JsonLDWriter writer = new JsonLDWriter(RDFFormat.JSONLD_PRETTY);
//						writer.wri
					}
					if(true) { return; };
				}
				
				
				List<String> filenames = new ArrayList<>(args.getNonOptionArgs());//args.getOptionValues("sparql");



				// If an out file is given, it is only overridden if the process succeeds.
				// This allows scripts to 'update' an existing file without
				// corrupting it if something goes wrong
				String outFilename = Optional.ofNullable(args.getOptionValues("o"))
						.orElse(Collections.emptyList()).stream()
						.findFirst().orElse(null);

				String ioFilename = Optional.ofNullable(args.getOptionValues("io"))
						.orElse(Collections.emptyList()).stream()
						.findFirst().orElse(null);

				// io uses the given file also as input
				if(ioFilename != null) {
					if(outFilename != null) {
						throw new RuntimeException("--o and --io are mutually exclusive");
					}
					
					outFilename = ioFilename;
					filenames.listIterator().add(outFilename);
				}

				
				Path outFile;
				Path tmpFile;
				OutputStream operationalOut;
				if(!Strings.isNullOrEmpty(outFilename)) {
					outFile = Paths.get(outFilename).toAbsolutePath();
					if(Files.exists(outFile) && !Files.isWritable(outFile)) { 
						throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
					}
					
					Path parent = outFile.getParent();
					String tmpName = "." + outFile.getFileName().toString() + ".tmp";
					tmpFile = parent.resolve(tmpName);

					operationalOut = Files.newOutputStream(tmpFile,
							StandardOpenOption.CREATE,
							StandardOpenOption.WRITE,
							StandardOpenOption.TRUNCATE_EXISTING);
					
					// Register a shutdown hook to delete the temporary file
					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
						try {
							operationalOut.close();
							Files.deleteIfExists(tmpFile);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}));
					
				} else {
					operationalOut = System.out;
					outFile = null;
					tmpFile = null;
				}
								
				
				SparqlStmtProcessor processor = new SparqlStmtProcessor();
				processor.setLogTime(true);
				processor.setShowQuery(args.containsOption("q"));
				processor.setShowAlgebra(args.containsOption("a"));
				
				Collection<RDFFormat> availableOutRdfFormats = RDFWriterRegistry.registered();
				String tmpOutFormat = Optional.ofNullable(args.getOptionValues("w"))
						.orElse(Collections.emptyList()).stream()
						.findFirst().orElse(null);

				String optOutFormat = args.containsOption("jq")
						? "jq"
						: tmpOutFormat;
				boolean jsonFlat = args.containsOption("flat");
				int depth = Integer.valueOf(Optional.ofNullable(args.getOptionValues("depth"))
						.orElse(Collections.emptyList()).stream()
						.findFirst().orElse("3"));

				PrefixMapping pm = new PrefixMappingImpl();
				pm.setNsPrefixes(DefaultPrefixes.prefixes);
				JenaExtensionUtil.addPrefixes(pm);

				JenaExtensionHttp.addPrefixes(pm);
				
				SPARQLResultSink sink;
				RDFFormat outFormat = null;
				if(optOutFormat != null) {
					if(optOutFormat.equals("jq")) {
						sink = new SPARQLResultVisitorSelectJsonOutput(null, depth, jsonFlat, gson);
					} else {
				        outFormat = availableOutRdfFormats.stream()
				        		.filter(f -> f.toString().equalsIgnoreCase(optOutFormat))
				        		.findFirst()
								.orElseThrow(() -> new RuntimeException("Unknown format: " + optOutFormat + " Available: " + availableOutRdfFormats));
				        
						Sink<Quad> quadSink = SparqlStmtUtils.createSink(outFormat, operationalOut, pm);
						sink = new SPARQLResultSinkQuads(quadSink);
					}			        
				} else {
					Sink<Quad> quadSink = SparqlStmtUtils.createSink(outFormat, operationalOut, pm);
					sink = new SPARQLResultSinkQuads(quadSink);
				}

				boolean isUnionDefaultGraphMode = args.containsOption("u");

//				System.out.println(outFormat);
				
				//SPARQLResultVisitor
				//Sink<Quad> sink = SparqlStmtUtils.createSink(outFormat, System.out);
				

				// TODO Replace with our RDFDataMgrEx 
				

				// Extended SERVICE <> keyword implementation
				JenaExtensionFs.registerFileServiceHandler();

				Prologue p = new Prologue(pm);
				SparqlQueryParser queryParser = SparqlQueryParserWrapperSelectShortForm.wrap(
						SparqlQueryParserImpl.create(Syntax.syntaxARQ, p));

				SparqlUpdateParser updateParser = SparqlUpdateParserImpl
						.create(Syntax.syntaxARQ, new Prologue(p));

				SparqlStmtParser sparqlParser = new SparqlStmtParserImpl(queryParser, updateParser, false);
				//SparqlStmtParser parser = SparqlStmtParserImpl.create(Syntax.syntaxARQ, pm, false);

				
				// System.out.println("ARGS: " + args.getOptionNames());
				Dataset dataset = DatasetFactory.create();
				try(RDFConnection actualConn = RDFConnectionFactoryEx.wrapWithContext(
						RDFConnectionFactoryEx.wrapWithQueryParser(RDFConnectionFactory.connect(dataset),
						str -> {
							SparqlStmt stmt = sparqlParser.apply(str);
							SparqlStmt r = SparqlStmtUtils.applyNodeTransform(stmt, x -> NodeUtils.substWithLookup(x, System::getenv));							 
							return r;
						}))) {

				
//				RDFConnection connQuery = isUnionDefaultGraphMode
//						? RDFConnectionFactory.connect(DatasetFactory.wrap(dataset.getUnionModel()))
//						: null;
//				if(true) {
//					String queryStr = "" +
//						"SELECT * { BIND(sys:nextLong() AS ?rowNum) BIND(sys:nextLong() AS ?rowNum2) BIND(sys:benchmark('SELECT * { ?s eg:foobar ?o }') AS ?x) }";
//for(int i = 0; i < 10; ++i) {
//						try(QueryExecution qe = conn.query(queryStr)) {
//							ResultSet rs = qe.execSelect();
//							ResultSetFormatter.outputAsCSV(System.out, rs);
//						}
//}
//						return;
//				}
				
				
				if (filenames == null || filenames.isEmpty()) {
					throw new RuntimeException(
							"No SPARQL files specified.");
				}

				Stopwatch sw = Stopwatch.createStarted();

				Path cwd = null;

//				String streamKey = "stream";

				for (String filename : filenames) {
					logger.info("Processing argument '" + filename + "'");
					
					
					
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

						// Has the argument been processed?
						boolean isProcessed = false;

						// Try as RDF file
						try(TypedInputStream tmpIn = RDFDataMgrEx.open(filename, Arrays.asList(Lang.TRIG, Lang.NQUADS))) {
							InputStream in = tmpIn.getInputStream();
							
							String contentType = tmpIn.getContentType();
							logger.info("Detected format: " + contentType);
							Lang rdfLang = contentType == null ? null : RDFLanguages.contentTypeToLang(contentType);
							
							//Lang rdfLang = RDFDataMgr.determineLang(filename, null, null);
							if(rdfLang != null) {
								isProcessed = true;

								if(RDFLanguages.isTriples(rdfLang)) {
									// What to do if stream mode is active?
									
									
									Model tmp = ModelFactory.createDefaultModel();
									//InputStream in = SparqlStmtUtils.openInputStream(filename);
									// FIXME Validate we are really using turtle here
									parseTurtleAgainstModel(tmp, pm, in);
									// Copy any prefixes from the parse back to our global prefix mapping
									pm.setNsPrefixes(tmp);
								
		//							tmp.setNsPrefixes(pm);
		//							RDFDataMgr.read(tmp, filename);
									
									// FIXME control which graph to load into - by default its the default graph
									logger.info("RDF File detected, loading into default graph");
									actualConn.load(tmp);
								} else if(RDFLanguages.isQuads(rdfLang)) {
									Dataset tmp = DatasetFactory.create();
									// InputStream in = SparqlStmtUtils.openInputStream(filename);
									if(in == null) {
										throw new FileNotFoundException(filename);
									}
									// FIXME Validate we are really using turtle here
									parseTrigAgainstDataset(tmp, pm, in);
									// Copy any prefixes from the parse back to our global prefix mapping
									
									Model m = tmp.getDefaultModel();
									if(m != null) {
										logger.info("Loading default graph");
										actualConn.load(m);
										pm.setNsPrefixes(m);
									}
	
									logger.info("Loading named graphs");
									int i = 0;
									Iterator<String> it = tmp.listNames();
									while(it.hasNext()) {
										String name = it.next();
										m = tmp.getNamedModel(name);
										if(m != null) {
											++i;
											//logger.info("Loading named graph " + name);
											actualConn.load(name, m);
											pm.setNsPrefixes(m);
										}
									}
									logger.info("Loaded " + i + " named graphs");
								
		//							tmp.setNsPrefixes(pm);
		//							RDFDataMgr.read(tmp, filename);
									
									// FIXME control which graph to load into - by default its the default graph
								} else {
									throw new RuntimeException("Unknown lang: " + rdfLang);
								}

							}
						} catch(Exception e) {
							logger.info("Probing for type of " + filename + ": not an RDF file");
						}
						
						
						if(!isProcessed) {
							
							// Check whether the argument is an inline sparql statement
							Iterator<SparqlStmt> it;
							SparqlStmtIterator itWithPos = null;
							
							try {
								String baseIri = cwd == null ? null : cwd.toUri().toString();
								it = itWithPos = SparqlStmtUtils.processFile(pm, filename, baseIri);
							} catch(IOException e) {

								try {
									SparqlStmt sparqlStmt = sparqlParser.apply(filename);
									it = Collections.singletonList(sparqlStmt).iterator();
								} catch(ARQException f) {
									// Possibly not a sparql query
									Throwable c = f.getCause();
									if(c instanceof QueryParseException) {
										QueryParseException qpe = (QueryParseException)c;
										boolean mentionsEncounteredSlash = Optional.ofNullable(qpe.getMessage())
												.orElse("").contains("Encountered: \"/\"");

										if(qpe.getLine() > 1 || (!mentionsEncounteredSlash && qpe.getColumn() > 1)) {
											throw new RuntimeException(filename + " could not be openend and failed to parse as SPARQL query", f);
										}
									}

									throw new IOException("Could not open " + filename, e);
								}
							}
								
							while(it.hasNext()) {
								if(itWithPos != null) {
									logger.info("Processing SPARQL statement at line " + itWithPos.getLine() + ", column " + itWithPos.getColumn());
								} else {
									logger.info("Processing inline SPARQL argument " + filename);
								}

								SparqlStmt stmt = it.next();
								
								 if(isUnionDefaultGraphMode) {
									 stmt = SparqlStmtUtils.applyOpTransform(stmt, Algebra::unionDefaultGraph);
								 }
								 

								processor.processSparqlStmt(actualConn, stmt, sink);
							}
						}
					}
				}
				
				sink.flush();
				sink.close();				
				
				logger.info("SPARQL overall execution finished after " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");
				
				if(outFile != null) {
					operationalOut.flush();
					operationalOut.close();
					Files.move(tmpFile, outFile, StandardCopyOption.REPLACE_EXISTING);
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


				
				// QueryExecutionFactory qef = FluentQueryExecutionFactory.from(model)
				// .config()
				// //.withParser(sparqlStmtParser)
				// //.withPrefixes(PrefixMapping.Extended, false)
				// .end().create();

				if (args.containsOption("server")) {
					SparqlService sparqlService = FluentSparqlService.from(actualConn).create();

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
				}
			};
		}
	}

//	public static void processSparqlStmtWrapper(RDFConnection conn, SparqlStmt stmt, boolean showAlgebra, SPARQLResultVisitor sink) {
//		Stopwatch sw2 = Stopwatch.createStarted();
//		processSparqlStmt(conn, stmt, showAlgebra, sink);
//		logger.info("SPARQL stmt execution finished after " + sw2.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");
//	}
	
	// TODO Maybe show algebra is better for trace logging?
	
	

	public static void init() {
		
		// Disable creation of a derby.log file ; triggered by the GeoSPARQL module
		System.setProperty("derby.stream.error.field", "org.aksw.sparql_integrate.cli.DerbyUtil.DEV_NULL");
		
		// Init geosparql module
		GeoSPARQLConfig.setupNoIndex();

		// Retain blank node labels
		// Note, that it is not sufficient to enable only input or output bnode labels
		ARQ.enableBlankNodeResultLabels();
		
		// Jena (at least up to 3.11.0) handles pseudo iris for blank nodes on the parser level
		// {@link org.apache.jena.sparql.lang.ParserBase}
		// This means, that blank nodes in SERVICE clauses would not be passed on as such
		ARQ.setFalse(ARQ.constantBNodeLabels);
		
		JenaExtensionHttp.register(() -> HttpClientBuilder.create().build());		
	}

	public static void main(String[] args) throws URISyntaxException, FileNotFoundException, IOException, ParseException {
		init();
				
		if(false) {
			// https://issues.apache.org/jira/browse/JENA-1843
			Query expected = QueryFactory.create("SELECT * { ?s ?p ?o BIND(?s AS ?x) BIND(?x AS ?y) }");
			Query actual = OpAsQuery.asQuery(Algebra.compile(expected));
			System.out.println(expected);
			System.out.println(actual);
		}

		if(false) {
			// https://issues.apache.org/jira/browse/JENA-1844
			Query expected = QueryFactory.create("SELECT * { ?s ?p ?o BIND(?s AS ?x) } ORDER BY ?s");
			Query actual = OpAsQuery.asQuery(Algebra.compile(expected));
			System.out.println(expected);
			System.out.println(actual);
		}
		
		if(false) {
			// https://issues.apache.org/jira/browse/JENA-1848
			System.out.println("Benchmarking trig loading/writing");
			Stopwatch sw = Stopwatch.createStarted();
			Dataset ds = RDFDataMgr.loadDataset("../ngs/test-data.trig");
			System.out.println("Loaded " + Iterators.size(ds.listNames()) + " graphs in in " + sw.elapsed(TimeUnit.SECONDS) + " seconds");
			sw.reset().start();
			RDFDataMgr.write(new NullOutputStream(), ds, RDFFormat.TRIG_PRETTY);
			System.out.println("Writing time in seconds: " + sw.elapsed(TimeUnit.SECONDS));
		}

		if(false) {
			return;
		}

		
		// ARQ.setTrue(ARQ.inputGraphBNodeLabels); // Only this gives in the output bnode labels such as b2
		
		// Only this gives in the output bnode labels such as e6fb382e90ae1d44f42275492ab4c907
		// This means, that input blank nodes were renamed
		// ARQ.setTrue(ARQ.outputGraphBNodeLabels);
		

//		if(true) {
//			try(RDFConnection conn = RDFConnectionFactory.connect("http://localhost:8890/sparql")) {
//				QueryExecution qe = conn.query("SELECT * { ?s ?p ?o FILTER(isBlANK(?s)) }");
//				ResultSet rs = qe.execSelect();
////				while(rs.hasNext()) {
////					Binding b = rs.nextBinding();
////					//System.out.println(b);
////				}
//		        ResultsWriter.create()
//		        .context(ARQ.getContext())
//	            .lang(ResultSetLang.SPARQLResultSetJSON)
//	            .write(System.out, rs);
//
//				//ResultSetFormatter.outputAsJSON(System.out, qe.execSelect());
//				//ResultSetMgr.write(System.out, rs, ResultSetLang.SPARQLResultSetJSON);
////				ResultSetMgr.write(System.out, rs, ResultSetLang.SPARQLResultSetCSV);
//				//				System.out.println(ResultSetFormatter.outputAsJSON(qe.execSelect()));
//			}
//			return;
//		}
//		if(true) {
//			String file = "/home/raven/tmp/strange-dbpedia-data.nt";
////			String file = "/home/raven/tmp/test.txt";
//			String str = Files.lines(Paths.get(file), Charset.forName("cesu-8")).collect(Collectors.joining("\n"));
//			System.out.println(str);
//			
//			return;
//			//Model m = RDFDataMgr.loadModel("/home/raven/tmp/strange-dbpedia-data.nt");
//			//RDFDataMgr.write(System.out, m, RDFFormat.TURTLE_PRETTY);
//		}
		
//		Function<String, SparqlStmt> parser = SparqlStmtParserImpl.create(Syntax.syntaxARQ, true);
//		parser.apply("INSERT { ?s ?p ?o } WHERE { <env://TEST> ?p ? o . {SELECT ?x {)

//		if(true) {
//			SparqlStmt stmt = SparqlStmtUtils.processFile(new PrefixMapping2(PrefixMapping.Extended), "env-test.sparql").next();
//			stmt = SparqlStmtUtils.applyNodeTransform(stmt, n -> SparqlStmtProcessor.substWithLookup(n, s -> s.equals("S") ? "http://foo" : null));
//			System.out.println(stmt);
//			return;
//		}
		
//		if(true) {
//			System.out.println(new URI("http://foo.bar/baz/bak//").resolve(".."));//.normalize());
//			return;
//		}
		

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
