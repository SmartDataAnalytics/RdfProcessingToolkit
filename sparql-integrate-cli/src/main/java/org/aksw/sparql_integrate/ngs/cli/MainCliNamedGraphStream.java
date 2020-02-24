package org.aksw.sparql_integrate.ngs.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.core.utils.ServiceUtils;
import org.aksw.jena_sparql_api.io.utils.SimpleProcessExecutor;
import org.aksw.jena_sparql_api.rx.FlowableTransformerLocalOrdering;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx.QuadEncoderMerge;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSink;
import org.aksw.jena_sparql_api.stmt.SPARQLResultSinkQuads;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.transform.result_set.QueryExecutionTransformResult;
import org.aksw.jena_sparql_api.utils.QueryUtils;
import org.aksw.sparql_integrate.cli.MainCliSparqlIntegrate;
import org.aksw.sparql_integrate.cli.MainCliSparqlStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.atlas.io.NullOutputStream;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.ext.com.google.common.collect.ImmutableSet;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WebContent;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableTransformer;
import io.reactivex.schedulers.Schedulers;
import jersey.repackaged.com.google.common.collect.Iterators;
import joptsimple.internal.Strings;

public class MainCliNamedGraphStream {
	
//	public static String toString(Dataset dataset, RDFFormat format) {
//	}
	
	public static String serialize(Node key, Dataset dataset, RDFFormat format) {		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RDFDataMgr.write(baos, dataset, format);
		String keyStr = key.isURI() ? key.getURI() : key.getLiteralValue().toString();
		String result = keyStr + " \t" + StringEscapeUtils.escapeJava(baos.toString());
		return result;
	}
	
	
	public static Dataset deserialize(String line, Lang lang) {
		int idx = line.indexOf('\t');
		String encoded = line.substring(idx + 1);
		String decoded = StringEscapeUtils.unescapeJava(encoded);
		InputStream in = new ByteArrayInputStream(decoded.getBytes());
		
		Dataset result = DatasetFactory.create();
		RDFDataMgr.read(result, in, lang);
		return result;
	}
	
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
			tmp = Objects.requireNonNull(SparqlStmtUtils.openInputStream(src), "Could not create input stream from " + src);
		}
		
		Flowable<Dataset> result = RDFDataMgrRx.createFlowableDatasets(() ->
			MainCliSparqlIntegrate.prependWithPrefixes(tmp, pm))
			// TODO Decoding of distinguished names should go into the util method
			.map(ds -> QueryExecutionTransformResult.applyNodeTransform(RDFDataMgrRx::decodeDistinguished, ds));

		return result;
	}
	
	
	public static void main2(String[] args) {
		String raw = "This is	a test \nYay";
		System.out.println(raw);
		System.out.println(StringEscapeUtils.escapeJava(raw));
		
		Random rand = new Random(0);
		List<String> strs = IntStream.range(0, 1000000)
			.mapToObj(i -> rand.nextInt(100) + "\t" + RandomStringUtils.randomAlphabetic(10))
			.collect(Collectors.toList());
		
		System.out.println("Got random strings");
		Flowable.fromIterable(strs)
			.compose(systemCall(Arrays.asList("/usr/bin/sort", "-h", "-t", "\t")))
			.timeout(60, TimeUnit.SECONDS)
			.blockingForEach(System.out::println);
		
		  //.count()
		//.blockingGet();
		
//		System.out.println(x);
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
		//CmdNgsConflate cmdConflate = new CmdNgsConflate();

		
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
			Function<Dataset, Dataset> mapper = inDs -> {
				
//				System.out.println("Sleeping thread " + Thread.currentThread());
//				try { Thread.sleep(500); } catch(InterruptedException e) { }
				
				Dataset out = DatasetFactory.create();

				List<String> names = Streams.stream(inDs.listNames()).collect(Collectors.toList());
				if(names.size() != 1) {
					logger.warn("Expected a single named graph, got " + names);
					return out;
				}
				String name = names.get(0);
				
				SPARQLResultSinkQuads sink = new SPARQLResultSinkQuads(out.asDatasetGraph()::add);
				try(RDFConnection conn = RDFConnectionFactory.connect(inDs)) {
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
				.map(e -> {
					Dataset tmp = mapper.apply(e.getKey());
					return Maps.immutableEntry(tmp, e.getValue());
				})
				.sequential()
				.compose(FlowableTransformerLocalOrdering.transformer(0l, i -> i + 1, Entry::getValue))
//				.doAfterNext(System.out::println)
				.map(Entry::getKey)
				;
			
			//flow.forEach(System.out::println);
			//RDFDataMgrRx.writeDatasets(flow, new NullOutputStream(), RDFFormat.TRIG);
			RDFDataMgrRx.writeDatasets(flow, System.out, RDFFormat.TRIG);
			break;
		}
		case "sort": {

			SparqlQueryParser keyQueryParser = SparqlQueryParserWrapperSelectShortForm.wrap(
					SparqlQueryParserImpl.create(pm));

			// SPARQL      : SELECT ?key { ?s eg:hash ?key }
			// Short SPARQL: ?key { ?s eg:hash ?key }
			// LDPath      : issue: what to use as the root?
			String keyArg = cmdSort.key;
			
			Function<Dataset, Node> keyMapper;
			

			if(keyArg != null && !keyArg.isEmpty()) {
				Query rawKeyQuery = keyQueryParser.apply(keyArg);
				QueryUtils.optimizePrefixes(rawKeyQuery);
				
				Query keyQuery = QueryUtils.applyOpTransform(rawKeyQuery, Algebra::unionDefaultGraph);

				
				List<Var> projectVars = rawKeyQuery.getProjectVars();
				if(projectVars.size() != 1) {
					throw new RuntimeException("Key query must have exactly 1 result var");
				}
				Var keyVar = projectVars.get(0);

				keyMapper = ds -> {
					QueryExecution qe = QueryExecutionFactory.create(keyQuery, ds);
					//QueryExecutionUtils.
					//SparqlRx.fetch
					List<Node> nodes = ServiceUtils.fetchList(qe, keyVar);
					
					Node r = Iterables.getFirst(nodes, NodeFactory.createLiteral(""));
					return r;
				};
			} else {
				keyMapper = ds -> {
					Iterator<Node> graphNames = ds.asDatasetGraph().listGraphNodes();
					Node r = Iterators.getNext(graphNames, NodeFactory.createLiteral(""));
					//Node r = NodeFactory.createURI(rn);
					return r;
				};
			}

			List<String> sortArgs = Lists.newArrayList("/usr/bin/sort", "-t", "\t");
			if(cmdSort.unique) {
				sortArgs.add("-u");
			}

			if(cmdSort.randomSort) {
				sortArgs.add("-R");
			} else {
				sortArgs.add("-h");
			}
			
			
			if(!Strings.isNullOrEmpty(cmdSort.bufferSize)) {
				sortArgs.add("-S");
				sortArgs.add(cmdSort.bufferSize);
			}
			
			RDFFormat fmt = RDFFormat.TRIG_PRETTY;

			Flowable<Dataset> flow = createNamedGraphStreamFromArgs(cmdSort.nonOptionArgs, null, pm)
				.map(ds -> {
					Node key = keyMapper.apply(ds);
					return Maps.immutableEntry(key, ds);
				})
				.map(e -> serialize(e.getKey(), e.getValue(), fmt))
				// sort by string before tab tabs, -h human-numeric
				.compose(systemCall(sortArgs))
				.map(str -> deserialize(str, fmt.getLang()));
			
			boolean merge = cmdSort.merge;
			if(merge) {
				QuadEncoderMerge merger = new RDFDataMgrRx.QuadEncoderMerge();
				Iterable<Dataset> pendingDs = () -> {
					Dataset ds = merger.getPendingDataset();
					Iterator<Dataset> r = ds.isEmpty()
							? ImmutableSet.<Dataset>of().iterator()
							: Iterators.singletonIterator(ds);
					return r;
				};
				
				flow = flow
						.map(merger::accept)
						.concatWith(Flowable.fromIterable(pendingDs));
			}
			
			
			RDFDataMgrRx.writeDatasets(flow, System.out, fmt);
			
//			List<String> noas = cmdSort.nonOptionArgs;
//			if(noas.size() != 1) {
//				throw new RuntimeException("Only one non-option argument expected for the artifact id");
//			}
//			String pattern = noas.get(0);

			break;
		}
		}

//		JCommander deploySubCommands = jc.getCommands().get("sort");
//
//		CommandDeployCkan cmDeployCkan = new CommandDeployCkan();
//		deploySubCommands.addCommand("ckan", cmDeployCkan);
	}

	
	public static FlowableTransformer<String, String> systemCall(List<String> args) {		
		return upstream -> {
			return Flowable.create(new FlowableOnSubscribe<String>() {
				@Override
				public void subscribe(FlowableEmitter<String> e) throws Exception {
					SimpleProcessExecutor.wrap(new ProcessBuilder(args))
						.executeReadLines(upstream, e);
				}
			}, BackpressureStrategy.BUFFER);
		};
	}

	
}


//

//flow
//	.map(ds -> Maps.immutableEntry(keyMapper.apply(ds), ds))
//	.map(e -> serialize(e.getKey(), e.getValue()))
//	.compose(composer)
//
//		
//		Subscriber<T> tmp = wrap(initiallyExpectedId, incrementSeqId, extractSeqId, e);
//		upstream.subscribe(tmp::onNext, tmp::onError, tmp::onComplete);
//	}
//}, BackpressureStrategy.BUFFER);
//
