package org.aksw.sparql_integrate.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.json.SPARQLResultVisitorSelectJsonOutput;
import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSink;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSinkQuads;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtIterator;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.algebra.TransformUnionQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.google.common.base.StandardSystemProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Flowable;

public class MainCliSparqlStream {
	private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlStream.class);

	
	public static SPARQLResultSink createSink(
			String optOutFormat,
			PrefixMapping pm,
			PrintStream operationalOut) {
		
		SPARQLResultSink result;

		Collection<RDFFormat> availableOutRdfFormats = RDFWriterRegistry.registered();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		Dataset outDataset = DatasetFactoryEx.createInsertOrderPreservingDataset();
		RDFFormat outFormat = null;
		if(optOutFormat != null) {
			if(optOutFormat.equals("jq")) {
				int depth = 3;
				boolean jsonFlat = false;
				result = new SPARQLResultVisitorSelectJsonOutput(null, depth, jsonFlat, gson);
			} else {
		        outFormat = availableOutRdfFormats.stream()
		        		.filter(f -> f.toString().equalsIgnoreCase(optOutFormat))
		        		.findFirst()
						.orElseThrow(() -> new RuntimeException("Unknown format: " + optOutFormat + " Available: " + availableOutRdfFormats));
		        
				Sink<Quad> quadSink = SparqlStmtUtils.createSink(outFormat, operationalOut, pm, outDataset);
				result = new SPARQLResultSinkQuads(quadSink);
			}			        
		} else {
			Sink<Quad> quadSink = SparqlStmtUtils.createSink(outFormat, operationalOut, pm, outDataset);
			result = new SPARQLResultSinkQuads(quadSink);
		}

		return result;
	}

		
	public static BiConsumer<RDFConnection, SPARQLResultSink> createProcessor(
			//CommandMain cliArgs,
			List<String> args,
			PrefixMapping pm,
			boolean closeSink
			) throws FileNotFoundException, IOException, ParseException {
		
		List<BiConsumer<RDFConnection, SPARQLResultSink>> outerParts = new ArrayList<>();
		
		//List<String> args = cliArgs.nonOptionArgs;
		
//		processor.setShowQuery(args.containsOption("q"));
//		processor.setShowAlgebra(args.containsOption("a"));

//		String tmpOutFormat = Optional.ofNullable(args.getOptionValues("w"))
//				.orElse(Collections.emptyList()).stream()
//				.findFirst().orElse(null);
//
//		String optOutFormat = args.containsOption("jq")
//				? "jq"
//				: tmpOutFormat;
//

		
		Path cwd = null;
		for (String filename : args) {
			logger.info("Loading argument '" + filename + "'");

			if(filename.startsWith(MainCliSparqlIntegrate.cwdKey)) {
				String cwdValue = filename.substring(MainCliSparqlIntegrate.cwdKey.length()).trim();

				if(cwd == null) {
					cwd = Paths.get(StandardSystemProperty.USER_DIR.value());
				}
				
				cwd = cwd.resolve(cwdValue);
				logger.info("Pinned working directory to " + cwd);
			} else if(filename.equals(MainCliSparqlIntegrate.cwdResetCwd)) {
				// If cwdValue is an empty string, reset the working directory
				logger.info("Unpinned working directory");

				cwd = null;
			}
			
			String baseIri = cwd == null ? null : cwd.toUri().toString();
			
			// Prevent concurrent modifications of prefixes;
			// processFile will add encountered prefixes
			PrefixMapping copy = new PrefixMappingImpl();
			copy.setNsPrefixes(pm);
			
			SparqlStmtIterator it = SparqlStmtUtils.processFile(copy, filename, baseIri);
		

			List<SparqlStmt> stmts = new ArrayList<>();

			while(it.hasNext()) {
				logger.info("Loading SPARQL statement at line " + it.getLine() + ", column " + it.getColumn());
				SparqlStmt stmt = it.next();
				stmt = SparqlStmtUtils.optimizePrefixes(stmt);
				
//				 if(cliArgs.isUnionDefaultGraphMode) {
					 stmt = SparqlStmtUtils.applyOpTransform(stmt, op -> Transformer
							 .transformSkipService(new TransformUnionQuery(), op));
//				 }
					 //Algebra.unionDefaultGraph(op)
				 stmts.add(stmt);
			}

			outerParts.add((conn, _sink) -> {
				SparqlStmtProcessor stmtProcessor = new SparqlStmtProcessor();

				// String inFile = filename;
				// logger.info("Applying '" + inFile + "'");

				for(SparqlStmt stmt : stmts) {
					// Some SPARQL query features are not thread safe - clone them!
					SparqlStmt cloneStmt = stmt.clone();
					stmtProcessor.processSparqlStmt(conn, cloneStmt, _sink);
				}				
			});

		}		
		
		BiConsumer<RDFConnection, SPARQLResultSink> result = (conn, sink) -> {
			//SPARQLResultSink sink = createSink(optOutFormat, pm, operationalOut);

			for(BiConsumer<RDFConnection, SPARQLResultSink> part : outerParts) {
				part.accept(conn, sink);
			}

			sink.flush();
			if(closeSink) {
				try {
					sink.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			
		};
		
		return result;
	}
	
	/**
	 * First non-option argument is interpreted as the input stream
	 * 
	 * sparql-stream input.trig *.sparql
	 * 
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
		CommandMain cm = new CommandMain();

		JCommander jc = new JCommander.Builder()
	    	  .addObject(cm)
	    	  .build();

		jc.parse(args);
		
		if(cm.nonOptionArgs.isEmpty()) {
			throw new RuntimeException("Need at least one non-option argument as input");
		}

		String src = cm.nonOptionArgs.get(0);

		// TODO Reuse code from sparql integrate
		
		MainCliSparqlIntegrate.init();
		
		PrefixMapping pm = new PrefixMappingImpl();
		pm.setNsPrefixes(DefaultPrefixes.prefixes);
		JenaExtensionUtil.addPrefixes(pm);
		JenaExtensionHttp.addPrefixes(pm);


		//Function<Dataset, Dataset> processor = null;
		PrintStream operationalOut = System.out;
		String optOutFormat = "trig/pretty";

		SPARQLResultSink sink = createSink(optOutFormat, pm, operationalOut);
		// Skip first argument
		BiConsumer<RDFConnection, SPARQLResultSink> consumer = createProcessor(
				cm.nonOptionArgs.subList(1, cm.nonOptionArgs.size()),
				pm,
				true);
		
		
		Flowable<Dataset> datasets = RDFDataMgrRx.createFlowableDatasets(() ->
			MainCliSparqlIntegrate.prependWithPrefixes(
					SparqlStmtUtils.openInputStream(src), pm));

		datasets.forEach(ds -> {
			Dataset indexedCopy = DatasetFactory.wrap(DatasetGraphFactory.cloneStructure(ds.asDatasetGraph()));
			try(RDFConnection conn = RDFConnectionFactory.connect(indexedCopy)) { 
				consumer.accept(conn, sink);
			}
		});
	}
}
