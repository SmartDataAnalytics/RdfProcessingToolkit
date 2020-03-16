package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CmdNgsMap {
	/**
	 * sparql-pattern file
	 * 
	 */
	@Parameter(names={"-s", "--sparql"}, description="sparql file or statement(s)")
	public List<String> stmts = new ArrayList<>();

	@Parameter(names={"-t", "--service-timeout"}, description="connect and/or query timeout in ms. E.g -t 1000 or -t 1000,2000")
	public String serviceTimeout = null;

	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	

	
	
//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;
}
