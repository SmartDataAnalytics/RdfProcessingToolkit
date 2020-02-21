package org.aksw.sparql_integrate.ngs.cli;

import java.util.List;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.sparql_integrate.cli.MainCliSparqlIntegrate;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WebContent;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;

import com.beust.jcommander.JCommander;

import io.reactivex.Flowable;

public class MainCliNamedGraphStream {

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

		
		// CommandCommit commit = new CommandCommit();
		JCommander jc = JCommander.newBuilder()
				.addObject(cmdMain)
				.addCommand("sort", cmdSort)
				.addCommand("head", cmdHead)
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
