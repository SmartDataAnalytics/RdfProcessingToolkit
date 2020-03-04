package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CmdNgsMap {
	/**
	 * sparql-pattern file
	 * 
	 */
	@Parameter(names={"-s", "--sparql"}, description="sparql statements")
	public List<String> stmts = new ArrayList<>();

	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	

//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;
}
