package org.aksw.sparql_integrate.ngs.cli.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserWrapperSelectShortForm;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsMap;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSort;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.lang.arq.ParseException;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

public class NamedGraphStreamOps {

	/**
		 * 
		 * @param cmdSort
		 * @param keyQueryParser
		 * @param format Serialization format when passing data to the system sort command
		 * @return
		 */
		public static FlowableTransformer<Dataset, Dataset> createSystemSorter(
				CmdNgsSort cmdSort,
				SparqlQueryParser keyQueryParser) {
			String keyArg = cmdSort.key;
			
			Function<? super SparqlQueryConnection, Node> keyMapper = MainCliNamedGraphStream.createKeyMapper(keyArg, keyQueryParser, MainCliNamedGraphStream.DISTINCT_NAMED_GRAPHS);
			
	
//			keyQueryParser = keyQueryParser != null
//					? keyQueryParser
//					: SparqlQueryParserWrapperSelectShortForm.wrap(SparqlQueryParserImpl.create(DefaultPrefixes.prefixes));
	
			// SPARQL      : SELECT ?key { ?s eg:hash ?key }
			// Short SPARQL: ?key { ?s eg:hash ?key }
			// LDPath      : issue: what to use as the root?
	
	
			List<String> sortArgs = SysCalls.createDefaultSortSysCall(cmdSort);
	
			return DatasetFlowOps.sysCallSort(keyMapper, sortArgs, cmdSort.merge);
		}

	public static void map(PrefixMapping pm, CmdNgsMap cmdMap)
			throws FileNotFoundException, IOException, ParseException {
		Flowable<Dataset> flow = MainCliNamedGraphStream.mapCore(pm, cmdMap);
		
		Consumer<List<Dataset>> writer = RDFDataMgrRx.createDatasetBatchWriter(System.out, RDFFormat.TRIG_PRETTY);
	
		flow
			.buffer(1000)
			//.timeout(1, TimeUnit.SECONDS)
			.blockingForEach(writer::accept)
			;
	
	//flow.blockingForEach(System.out::print);
	
	//flow.forEach(System.out::println);
	// RDFDataMgrRx.writeDatasets(flow, new NullOutputStream(), RDFFormat.TRIG);
	//RDFDataMgrRx.writeDatasets(flow, System.out, RDFFormat.TRIG_PRETTY);
	
	}

}
