package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * Filter named graphs by a SPARQL query
 * 
 * @author raven
 *
 */
public class CmdNgFilter {
	/**
	 * sparql-pattern file
	 * 
	 */
	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();
}
