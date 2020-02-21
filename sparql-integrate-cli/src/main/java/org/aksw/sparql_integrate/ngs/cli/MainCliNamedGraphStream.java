package org.aksw.sparql_integrate.ngs.cli;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.FlowableTransformerLocalOrdering;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSink;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSinkQuads;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.sparql_integrate.cli.MainCliSparqlIntegrate;
import org.aksw.sparql_integrate.cli.MainCliSparqlStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WebContent;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class MainCliNamedGraphStream {
	private static final Logger logger = LoggerFactory.getLogger(MainCliNamedGraphStream.class);
	
	/**
	 * Default procedure to obtain a stream of named graphs from a
	 * list of non-option arguments
	 * 
	 * If the list is empty or the first argument is '-' data will be read from stdin
	 * @param args
	 */
	public static Flowable<Dataset> createNamedGraphStreamFromArgs(
			List<String> inOutArgs,
			String fmtHint,
			PrefixMapping pm) {
		//Consumer<RDFConnection> consumer = createProcessor(cm, pm);
	
		String src;
		if(inOutArgs.isEmpty()) {
			src = null;
		} else {
			String first = inOutArgs.get(0);
			src = first.equals("-") ? null : first;
		}
	
		boolean useStdIn = src == null;
		
		TypedInputStream tmp;
		if(useStdIn) {
			// Use the close shield to prevent closing stdin on .close()
			tmp = new TypedInputStream(
					new CloseShieldInputStream(System.in),
					WebContent.contentTypeTriG); 
		} else {
			tmp = SparqlStmtUtils.openInputStream(src);
		}
		
		Flowable<Dataset> result = RDFDataMgrRx.createFlowableDatasets(() ->
		MainCliSparqlIntegrate.prependWithPrefixes(tmp, pm));
			
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		
		PrefixMapping pm = new PrefixMappingImpl();
		pm.setNsPrefixes(DefaultPrefixes.prefixes);
		JenaExtensionUtil.addPrefixes(pm);
		JenaExtensionHttp.addPrefixes(pm);


		CmdNgMain cmdMain = new CmdNgMain();
		CmdNgsSort cmdSort = new CmdNgsSort();
		CmdNgsHead cmdHead = new CmdNgsHead();
		CmdNgsMap cmdMap = new CmdNgsMap();

		
		// CommandCommit commit = new CommandCommit();
		JCommander jc = JCommander.newBuilder()
				.addObject(cmdMain)
				.addCommand("sort", cmdSort)
				.addCommand("head", cmdHead)
				.addCommand("map", cmdMap)
				.build();

		jc.parse(args);

        if (cmdMain.help) {
            jc.usage();
            return;
        }

		String cmd = jc.getParsedCommand();
		switch (cmd) {
		case "head": {
			// parse the numRecord option
			if(cmdHead.numRecords < 0) {
				throw new RuntimeException("Negative values not yet supported");
			}
			
			Flowable<Dataset> flow = createNamedGraphStreamFromArgs(cmdHead.nonOptionArgs, null, pm)
				.limit(cmdHead.numRecords);
			
			RDFDataMgrRx.writeDatasets(flow, System.out, RDFFormat.TRIG_PRETTY);
			
			
			break;
		}
		case "map": {
			BiConsumer<RDFConnection, SPARQLResultSink> processor =
					MainCliSparqlStream.createProcessor(cmdMap.stmts, pm);					

			//Sink<Quad> quadSink = SparqlStmtUtils.createSink(RDFFormat.TURTLE_PRETTY, System.err, pm);			
			Function<Dataset, Dataset> mapper = ind -> {
				
//				System.out.println("Sleeping thread " + Thread.currentThread());
//				try { Thread.sleep(500); } catch(InterruptedException e) { }
				
				Dataset out = DatasetFactory.create();

				List<String> names = Streams.stream(ind.listNames()).collect(Collectors.toList());
				if(names.size() != 1) {
					logger.warn("Expected a single named graph, got " + names);
					return out;
				}
				String name = names.get(0);
				
				SPARQLResultSinkQuads sink = new SPARQLResultSinkQuads(out.asDatasetGraph()::add);
				try(RDFConnection conn = RDFConnectionFactory.connect(ind)) {
					processor.accept(conn, sink);
				}
				
				// The input is guaranteed to be only a single named graph
				// If any data was generated in the out's default graph,
				// transfer it to a graph with the input name
				Model defaultModel = out.getDefaultModel();
				if(!defaultModel.isEmpty()) {
					Model copy = ModelFactory.createDefaultModel();
					copy.add(defaultModel);
					defaultModel.removeAll();
					//out.setDefaultModel(ModelFactory.createDefaultModel());
					out.addNamedModel(name, copy);
				}
				
				return out;
			};
			
			Flowable<Dataset> flow = createNamedGraphStreamFromArgs(cmdMap.nonOptionArgs, null, pm)
				// zipWithIndex
				.zipWith(() -> LongStream.iterate(0, i -> i + 1).iterator(), Maps::immutableEntry)
				.parallel()
				.runOn(Schedulers.computation())
				//.observeOn(Schedulers.computation())
				.map(e -> Maps.immutableEntry(mapper.apply(e.getKey()), e.getValue()))
				.sequential()
				.compose(FlowableTransformerLocalOrdering.transformer(0l, i -> i + 1, Entry::getValue))
//				.doAfterNext(System.out::println)
				.map(Entry::getKey)
				;
			
			//flow.forEach(System.out::println);
			RDFDataMgrRx.writeDatasets(flow, System.out, RDFFormat.TRIG);
			break;
		}
		case "sort": {
			List<String> noas = cmdSort.nonOptionArgs;
			if(noas.size() != 1) {
				throw new RuntimeException("Only one non-option argument expected for the artifact id");
			}
			String pattern = noas.get(0);

			break;
		}
		}

//		JCommander deploySubCommands = jc.getCommands().get("sort");
//
//		CommandDeployCkan cmDeployCkan = new CommandDeployCkan();
//		deploySubCommands.addCommand("ckan", cmDeployCkan);
	}
}
