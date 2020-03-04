package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * Probe the RDF language of some input
 * by trying out all available parsers
 * 
 * @author raven
 *
 */
public class CmdNgsProbe {
	/**
	 * sparql-pattern file
	 * 
	 */
//	@Parameter(names={"-n"}, description="numRecords")
//	public long numRecords = 10;

//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;

	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	
}
