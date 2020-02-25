package org.aksw.sparql_integrate.ngs.cli;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * List the top n named graphs
 * 
 * @author raven
 *
 */
public class CmdNgsHead {
	/**
	 * sparql-pattern file
	 * 
	 */
	@Parameter(names={"-n"}, description="numRecords")
	public long numRecords = 10;

	@Parameter(names={"-h", "--help"}, help = true)
	public boolean help = false;

	@Parameter(names={"-o", "--out-format"})
	public String outFormat = "trig/pretty";

	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	
}
